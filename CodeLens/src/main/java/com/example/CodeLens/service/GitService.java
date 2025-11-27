package com.example.CodeLens.service;

import com.example.CodeLens.model.ParsedJavaFile;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class GitService {

    private final JavaParserService javaParserService;
    private final JavaSourceScannerService javaSourceScannerService;
    private final JsonDumpService jsonDumpService;

    @Value("${app.ingestion.work-dir}")
    private String baseDirConfig;

    @Autowired
    public GitService(JavaParserService javaParserService,
                      JavaSourceScannerService javaSourceScannerService,
                      JsonDumpService jsonDumpService) {

        this.javaParserService = javaParserService;
        this.javaSourceScannerService = javaSourceScannerService;
        this.jsonDumpService = jsonDumpService;
    }

    public Path downloadRepository(String repoUrl) {
        validateUrl(repoUrl);

        Path destinationPath = createTargetDirectory(repoUrl);
        String repoName = extractRepoName(repoUrl);

        // NEW: JSON output directory for this specific repo
        Path jsonRepoFolder = Paths.get("codelens_data/jsons", repoName);
        createDirectory(jsonRepoFolder);

        try {
            System.out.println("Starting clone: " + repoUrl);
            System.out.println("Destination: " + destinationPath.toAbsolutePath());

            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(destinationPath.toFile())
                    .setDepth(1)
                    .setCloneAllBranches(false)
                    .call()
                    .close();

            // Scan .java files
            List<Path> javaFiles = javaSourceScannerService.scanForJavaFiles(destinationPath);

            for (Path javaFile : javaFiles) {
                ParsedJavaFile parsedJavaFile = javaParserService.parseJavaFile(javaFile);

                // ===== SAVE JSON INSIDE project folder =====
                String jsonName = javaFile.getFileName().toString() + ".json";
                Path jsonOutputPath = jsonRepoFolder.resolve(jsonName);

                jsonDumpService.dumpToJsonFile(parsedJavaFile, jsonOutputPath);

                System.out.println("Parsed: " + javaFile);
                System.out.println("Saved JSON → " + jsonOutputPath);

                parsedJavaFile.getClasses().forEach(clazz -> {
                    System.out.println("Class: " + clazz.getName());
                    clazz.getMethods().forEach(method ->
                            System.out.println("  Method: " + method.getName()));
                });

                System.out.println("=================================");
            }

            System.out.println("Clone successful!");
            return destinationPath;

        } catch (GitAPIException | IOException e) {
            deleteDirectory(destinationPath);
            throw new RuntimeException("Git Clone Failed: " + e.getMessage(), e);
        }
    }

    // ============================== INTERNAL HELPERS ==============================

    private void createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    private Path createTargetDirectory(String repoUrl) {
        String repoName = extractRepoName(repoUrl);
        String uniqueFolder = repoName + "-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            Path root = (baseDirConfig == null || baseDirConfig.isEmpty())
                    ? Paths.get(System.getProperty("java.io.tmpdir"), "codelens-repos")
                    : Paths.get(baseDirConfig);

            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            return root.resolve(uniqueFolder);

        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    private String extractRepoName(String url) {
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (name.endsWith(".git")) name = name.substring(0, name.length() - 4);
        return name;
    }

    private void validateUrl(String url) {
        if (url == null || !url.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("Invalid URL. Only GitHub HTTPS repos supported.");
        }
    }

    public void deleteDirectory(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                FileUtils.deleteDirectory(path.toFile());
                System.out.println("Cleaned up: " + path);
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to delete " + path);
        }
    }
}
