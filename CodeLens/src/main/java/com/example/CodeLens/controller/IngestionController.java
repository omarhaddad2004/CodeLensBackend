package com.example.CodeLens.controller;


import com.example.CodeLens.service.EnrichmentService;
import com.example.CodeLens.service.GitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/repo")
@RequiredArgsConstructor
public class IngestionController {

    private final GitService gitService;
    private final  EnrichmentService enrichmentService;

    @PostMapping("/download")
    public ResponseEntity<Map<String, Object>> downloadRepo(@RequestBody Map<String, String> payload) {
        String url = payload.get("url");
        Map<String, Object> response = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();

            // Call the service
            Path localPath = gitService.downloadRepository(url);

            long duration = System.currentTimeMillis() - startTime;
            double durationS = duration / 1000.0;

            response.put("status", "SUCCESS");
            response.put("message", "Repository downloaded successfully");
            response.put("local_path", localPath.toString());
            response.put("duration_s", durationS);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/enrich-data")
    public String StartEnrichment() {
        new Thread(() -> enrichmentService.runPipeline()).start();
        return "Pipeline started! Check console logs and 'enriched' folder.";
    }
}