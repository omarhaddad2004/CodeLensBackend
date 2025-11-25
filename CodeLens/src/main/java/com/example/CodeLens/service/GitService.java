package com.example.CodeLens.service;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class GitService {

    @Value("${app.ingestion.work-dir}")
    private String baseDirConfig;


    public Path downloadRepository(String repoUrl) {
        // Validate Input and the validateUrl in line 91
        validateUrl(repoUrl);

        //Generates a unique folder for this repo download.
        Path destinationPath = createTargetDirectory(repoUrl);

        try {
            //For debugging: prints which repo is being cloned.
            System.out.println("Starting clone: " + repoUrl);
            //Shows exactly where on disk the repo is being cloned.
            System.out.println("Destination: " + destinationPath.toAbsolutePath());

            // This is the Actual Clone(All the method in JGit)

            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(destinationPath.toFile()) //Where to put the cloned repo(Here we convert Path->File)
                    .setDepth(1)              //  Shallow clone (latest commit only)
                    .setCloneAllBranches(false) // Only clone the default branch (main/master), not all branches.
                    .call()//Executes the actual clone.
                    .close(); // Closes the Git object and releases any file locks.

            System.out.println("Clone successful!");
            return destinationPath;

        } catch (GitAPIException e) {
            // Cleanup on failure to keep disk clean
            deleteDirectory(destinationPath); //In line 103
            throw new RuntimeException("Git Clone Failed: " + e.getMessage(), e);
        }
    }


    private Path createTargetDirectory(String repoUrl) {
        String repoName = extractRepoName(repoUrl);//In line 84
        // Generates a unique folder name
        String uniqueFolder = repoName + "-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            // Resolve the path based on config or system temp
            Path root = (baseDirConfig == null || baseDirConfig.isEmpty())
                    ? Paths.get(System.getProperty("java.io.tmpdir"), "codelens-repos")
                    : Paths.get(baseDirConfig);

            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            //Combines root + unique folder
            return root.resolve(uniqueFolder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    private String extractRepoName(String url) {
        //If the URL ends with / remove it
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (name.endsWith(".git")) name = name.substring(0, name.length() - 4);
        return name;
    }


    private void validateUrl(String url) {
        if (url == null || !url.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("Invalid URL. Only 'https://github.com/...' is supported.");
        }
    }

    //delete the folder after analysis is done.
    public void deleteDirectory(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                FileUtils.deleteDirectory(path.toFile()); // Uses Commons IO(library)
                System.out.println("Cleaned up: " + path);
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to delete " + path);
        }
    }
}
