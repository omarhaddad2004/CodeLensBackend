package com.example.CodeLens.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class JavaSourceScannerService {
    public List<Path> scanForJavaFiles(Path repoPath) throws IOException {
        List<Path> javaFiles = new ArrayList<>();

        Files.walk(repoPath)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(javaFiles::add);
        return javaFiles;
    }
}
