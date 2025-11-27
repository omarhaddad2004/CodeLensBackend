package com.example.CodeLens.model;

import java.util.List;


public class ParsedJavaFile {

    private String filePath;
    private List<ParsedClass> classes;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public List<ParsedClass> getClasses() { return classes; }
    public void setClasses(List<ParsedClass> classes) { this.classes = classes; }
}

