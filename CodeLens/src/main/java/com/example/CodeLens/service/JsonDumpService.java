package com.example.CodeLens.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class JsonDumpService {

    private final ObjectMapper mapper;

    public JsonDumpService() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void dumpToJsonFile(Object value, Path outputPath) throws IOException {
        mapper.writeValue(outputPath.toFile(), value);
    }

    public String dumpToJsonString(Object value) throws JsonProcessingException {
        return mapper.writeValueAsString(value);
    }
}