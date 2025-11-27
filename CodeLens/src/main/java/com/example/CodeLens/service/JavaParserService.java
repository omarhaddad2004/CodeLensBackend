package com.example.CodeLens.service;

import com.example.CodeLens.model.ParsedClass;
import com.example.CodeLens.model.ParsedJavaFile;
import com.example.CodeLens.model.ParsedMethod;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class JavaParserService {
    private final JavaParser javaParser = new JavaParser();

    public ParsedJavaFile parseJavaFile(Path filePath) throws IOException {
        CompilationUnit cu = javaParser.parse(filePath).getResult().orElseThrow();

        ParsedJavaFile result = new ParsedJavaFile();
        result.setFilePath(filePath.toString());
        result.setClasses(new ArrayList<>());

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            ParsedClass c = new ParsedClass();
            c.setName(clazz.getNameAsString());

            // Methods
            List<ParsedMethod> methods = clazz.getMethods().stream()
                    .map(m -> new ParsedMethod(
                            m.getNameAsString(),
                            m.getParameters().toString(),
                            m.getTypeAsString(),
                            m.getRange().map(r -> r.begin.line + "-" + r.end.line).orElse(""),
                            m.getBody().map(b -> b.toString()).orElse("")
                    )).toList();
            c.setMethods(methods);

            // Fields
            List<String> fields = clazz.getFields().stream()
                    .map(f -> f.getVariables().toString())
                    .toList();

            c.setFields(fields);

            result.getClasses().add(c);
        });
        System.out.println(result.getClasses());
        return result;
    }
}
