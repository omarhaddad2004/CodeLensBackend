package com.example.CodeLens.service;

import com.example.CodeLens.dto.*;
import com.example.CodeLens.service.llm.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EnrichmentService {

    private final GeminiService aiService;
    private final ObjectMapper objectMapper;

    private final String INPUT_DIR = "codelens_data/jsons";
    private final String OUTPUT_DIR = "codelens_data/enriched";

    private final List<File> failedFiles = new ArrayList<>();

    public EnrichmentService(GeminiService aiService) {
        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
    }

    public void runPipeline() {
        File rootDir = new File(INPUT_DIR);

        // find all repo folders (sub-director)
        File[] repoFolders = rootDir.listFiles(File::isDirectory);

        if (repoFolders == null || repoFolders.length == 0) {
            System.err.println("No repository folders found in " + INPUT_DIR);
            return;
        }

        // loop through each repository
        for (File repoFolder : repoFolders) {
            System.out.println("==========================================");
            System.out.println("processing loop: " + repoFolder.getName());
            System.out.println("==========================================");

            processRepository(repoFolder);
        }
    }

    private void processRepository(File repoInputDir) {
        File[] files = repoInputDir.listFiles((d, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            System.out.println("No JSON files in " + repoInputDir.getName());
            return;
        }

        File repoOutputDir = new File(OUTPUT_DIR, repoInputDir.getName());
        repoOutputDir.mkdirs();

        failedFiles.clear(); // the retry list

        // ==== pass 1 ====
        System.out.println("=== STARTING PASS 1 (" + repoInputDir.getName() + ") ===");
        for (File file : files) {
            boolean success = processFileSafe(file, repoOutputDir, 4000);
            if (!success) {
                failedFiles.add(file);
                System.out.println(">> Added " + file.getName() + " to RETRY list.");
            }
        }

        // ==== paas 2 (retry) ========
        if (!failedFiles.isEmpty()) {
            System.out.println("\n=== STARTING PASS 2 (retrying " + failedFiles.size() + " files) ===");
            try { Thread.sleep(30000); } catch (InterruptedException e) {}

            for (File file : failedFiles) {
                System.out.println("Retrying: " + file.getName());
                processFileSafe(file, repoOutputDir, 10000);
            }
        }
    }

    private boolean processFileSafe(File inputFile, File outputDir, int sleepTime) {
        try {
            processFile(inputFile, outputDir, sleepTime);
            return true;
        } catch (HttpClientErrorException.TooManyRequests e) {
            System.err.println("gate limit hit: " + inputFile.getName());
            return false;
        } catch (Exception e) {
            System.err.println("generic error on " + inputFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private void processFile(File inputFile, File outputDir, int sleepTime) throws IOException {
        System.out.println("reading: " + inputFile.getName());

        ParserInputDto input = objectMapper.readValue(inputFile, ParserInputDto.class);
        List<EnrichedOutputDto.EnrichedMethod> enrichedMethods = new ArrayList<>();
        String className = "unknown";

        if (input.classes() != null) {
            for (ParserInputDto.ClassInfo clazz : input.classes()) {
                className = clazz.name();
                if (clazz.methods() != null) {
                    for (ParserInputDto.MethodInfo m : clazz.methods()) {

                        if (m.body() == null || m.body().trim().isEmpty()) continue;

                        String signature = m.name() + " " + m.body();

                        String summary = aiService.summarize(m.body());
                        sleep(sleepTime);

                        List<Double> vector = aiService.embed(signature + " " + summary);
                        sleep(sleepTime);

                        enrichedMethods.add(new EnrichedOutputDto.EnrichedMethod(
                                m.name(), m.body(), summary, vector
                        ));
                    }
                }
            }
        }

        if (!enrichedMethods.isEmpty()) {
            EnrichedOutputDto output = new EnrichedOutputDto(input.filePath(), className, enrichedMethods);
            // dynamic save
            File outputFile = new File(outputDir, inputFile.getName());

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, output);
            System.out.println("saved: " + outputFile.getPath());
        }
        else {
            System.out.println("skipping " + inputFile.getName() + " (no code)");
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }
}