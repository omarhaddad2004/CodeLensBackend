package com.example.CodeLens.dto;

import java.util.List;

public record EnrichedOutputDto(
        String fileName,
        String className,
        List<EnrichedMethod> methods
) {
    public record EnrichedMethod(
            String name,
            String body,
            String summary,
            List<Double> embedding
    ) {}
}