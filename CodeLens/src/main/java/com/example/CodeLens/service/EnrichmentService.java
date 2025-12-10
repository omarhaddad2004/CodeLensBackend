package com.example.CodeLens.service;

import com.example.CodeLens.model.elastic_model.CodeEntityDoc;
import com.example.CodeLens.service.elastic_service.ElasticsearchIndexingService;
import com.example.CodeLens.dto.ParserInputDto;
import com.example.CodeLens.service.llm.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
// REMOVED @RequiredArgsConstructor. We will write the constructor manually.
public class EnrichmentService {

    private final GeminiService aiService;
    // REMOVED 'final' so we don't need it in the constructor arguments.
    private final ObjectMapper objectMapper;
    private final ElasticsearchIndexingService elasticsearchIndexingService;

    private final String INPUT_DIR = "codelens_data/jsons";
    // OUTPUT_DIR is no longer needed for saving files, but kept if you want to revert.
    private final String OUTPUT_DIR = "codelens_data/enriched";

    private final List<File> failedFiles = new ArrayList<>();

    // --- NEW MANUAL CONSTRUCTOR ---
    // This tells Spring to inject the two services, and we create ObjectMapper ourselves.
    public EnrichmentService(GeminiService aiService,
                             ElasticsearchIndexingService elasticsearchIndexingService) {
        this.aiService = aiService;
        this.elasticsearchIndexingService = elasticsearchIndexingService;
        // Manually create the ObjectMapper instance here.
        this.objectMapper = new ObjectMapper();
    }
    // ------------------------------

    public void runPipeline() {
        File rootDir = new File(INPUT_DIR);

        // find all repo folders (sub-directory)
        File[] repoFolders = rootDir.listFiles(File::isDirectory);

        if (repoFolders == null || repoFolders.length == 0) {
            log.error("No repository folders found in {}", INPUT_DIR);
            return;
        }

        // loop through each repository
        for (File repoFolder : repoFolders) {
            log.info("==========================================");
            log.info("Processing repository: {}", repoFolder.getName());
            log.info("==========================================");

            processRepository(repoFolder);
        }
    }

    private void processRepository(File repoInputDir) {
        File[] files = repoInputDir.listFiles((d, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            log.warn("No JSON files found in {}", repoInputDir.getName());
            return;
        }

        // Output dir creation is no longer strictly necessary if we don't save to disk,
        // but keeping it doesn't hurt.
        File repoOutputDir = new File(OUTPUT_DIR, repoInputDir.getName());
        repoOutputDir.mkdirs();

        failedFiles.clear(); // the retry list

        // ==== pass 1 ====
        log.info("=== STARTING PASS 1 ({}) ===", repoInputDir.getName());
        for (File file : files) {
            // Passed null for outputDir as it's no longer used in processFile
            boolean success = processFileSafe(file, null, 4000);
            if (!success) {
                failedFiles.add(file);
                log.warn(">> Added {} to RETRY list.", file.getName());
            }
        }

        // ==== pass 2 (retry) ========
        if (!failedFiles.isEmpty()) {
            log.info("\n=== STARTING PASS 2 (retrying {} files) ===", failedFiles.size());
            sleep(30000);

            for (File file : failedFiles) {
                log.info("Retrying: {}", file.getName());
                // Passed null for outputDir
                processFileSafe(file, null, 10000);
            }
        }
    }

    private boolean processFileSafe(File inputFile, File outputDir, int sleepTime) {
        try {
            processFile(inputFile, outputDir, sleepTime);
            return true;
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("Rate limit hit processing {}: {}", inputFile.getName(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Generic error processing {}: {}", inputFile.getName(), e.getMessage(), e);
            return false;
        }
    }

    private void processFile(File inputFile, File outputDir, int sleepTime) throws IOException {
        log.info("Reading file: {}", inputFile.getName());

        ParserInputDto input = objectMapper.readValue(inputFile, ParserInputDto.class);

        // List to hold Elasticsearch documents for this file
        List<CodeEntityDoc> docsForEs = new ArrayList<>();

        String className = "unknown";

        if (input.classes() != null) {
            for (ParserInputDto.ClassInfo clazz : input.classes()) {
                className = clazz.name();
                if (clazz.methods() != null) {
                    for (ParserInputDto.MethodInfo m : clazz.methods()) {

                        if (m.body() == null || m.body().trim().isEmpty()) continue;

                        // Combine name and body for a richer embedding context
                        String signatureForEmbedding = m.name() + " " + m.body();

                        // 1. Get Summary from LLM
                        String summary = aiService.summarize(m.body());
                        sleep(sleepTime);

                        // 2. Get Vector Embedding from LLM
                        List<Double> vector = aiService.embed(signatureForEmbedding + " " + summary);
                        sleep(sleepTime);

                        // Create Elasticsearch Document
                        CodeEntityDoc doc = new CodeEntityDoc(
                                input.filePath(),
                                className,
                                m.name(),
                                m.body(),
                                summary,
                                vector
                        );
                        docsForEs.add(doc);
                    }
                }
            }
        }

        // Save batch to Elasticsearch instead of disk
        if (!docsForEs.isEmpty()) {
            elasticsearchIndexingService.saveBatch(docsForEs);
            log.info("Successfully indexed {} methods from file: {}", docsForEs.size(), inputFile.getName());
        }
        else {
            log.info("Skipping {} (no actionable code found).", inputFile.getName());
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}