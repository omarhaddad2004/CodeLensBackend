package com.example.CodeLens.service.elastic_service;

import com.example.CodeLens.model.elastic_model.CodeEntityDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeEntityRepository extends ElasticsearchRepository<CodeEntityDoc, String> {
    List<CodeEntityDoc> findByClassName(String className);

}