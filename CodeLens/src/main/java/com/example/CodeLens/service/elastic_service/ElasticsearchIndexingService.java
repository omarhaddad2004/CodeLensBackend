package com.example.CodeLens.service.elastic_service;

import com.example.CodeLens.model.elastic_model.CodeEntityDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchIndexingService {

    private final CodeEntityRepository repository;

    /**
     * Saves a batch of documents to Elasticsearch using bulk operations.
     * @param docs The list of documents to index.
     */
    public void saveBatch(List<CodeEntityDoc> docs) {
        if (docs == null || docs.isEmpty()) {
            log.warn("Attempted to save an empty batch to Elasticsearch.");
            return;
        }

        try {
            log.info("Sending batch of {} documents to Elasticsearch...", docs.size());
            repository.saveAll(docs);
            log.info("Successfully indexed {} documents.", docs.size());
        } catch (Exception e) {
            log.error("Failed to index batch in Elasticsearch", e);

            throw e;
        }
    }
}