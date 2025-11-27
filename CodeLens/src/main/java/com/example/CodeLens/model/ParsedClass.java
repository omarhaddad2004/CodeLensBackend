package com.example.CodeLens.model;

import java.util.List;

public class ParsedClass {
    private String name;
    private List<String> fields;
    private List<ParsedMethod> methods;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getFields() { return fields; }
    public void setFields(List<String> fields) { this.fields = fields; }

    public List<ParsedMethod> getMethods() { return methods; }
    public void setMethods(List<ParsedMethod> methods) { this.methods = methods; }
}
