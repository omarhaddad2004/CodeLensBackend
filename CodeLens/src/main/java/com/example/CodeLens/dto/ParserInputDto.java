package com.example.CodeLens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParserInputDto(
        String filePath,
        List<ClassInfo> classes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClassInfo(
            String name,
            List<MethodInfo> methods
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MethodInfo(
            String name,

            @JsonProperty("fullText")
            String body,

            String returnType
    ) {}
}