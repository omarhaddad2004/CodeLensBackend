package com.example.CodeLens.model;

public class ParsedMethod {
    private final String name;
    private final String parameters;
    private final String returnType;
    private final String lineRange;  // example: "12-22"
    private final String fullText;   // the method body text

    public ParsedMethod(String name, String parameters, String returnType, String lineRange, String fullText) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.lineRange = lineRange;
        this.fullText = fullText;
    }

    public String getName() {
        return name;
    }

    public String getParameters() {
        return parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getLineRange() {
        return lineRange;
    }

    public String getFullText() {
        return fullText;
    }
}

