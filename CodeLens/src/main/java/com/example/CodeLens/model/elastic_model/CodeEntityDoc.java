package com.example.CodeLens.model.elastic_model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.UUID;

@Document(indexName = "code-entities", createIndex = false)
public class CodeEntityDoc {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String filePath;

    @Field(type = FieldType.Keyword)
    private String className;

    @Field(type = FieldType.Keyword)
    private String methodName;

    @Field(type = FieldType.Text)
    private String methodBody;

    @Field(type = FieldType.Text, analyzer = "english")
    private String summary;


    @Field(type = FieldType.Dense_Vector, dims = 768)
    private List<Double> embedding;

    public CodeEntityDoc() {}

    public CodeEntityDoc(String filePath, String className, String methodName, String methodBody, String summary, List<Double> embedding) {
        // Generate a unique ID for ES.
        this.id = UUID.randomUUID().toString();
        this.filePath = filePath;
        this.className = className;
        this.methodName = methodName;
        this.methodBody = methodBody;
        this.summary = summary;
        this.embedding = embedding;
    }

    public String getId() { return id; }
    public String getFilePath() { return filePath; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public String getMethodBody() { return methodBody; }
    public String getSummary() { return summary; }
    public List<Double> getEmbedding() { return embedding; }
}