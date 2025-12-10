package com.example.CodeLens.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final RestClient restClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.base-url}")
    private String baseUrl;

    @Value("${gemini.model.chat}")
    private String chatModel;

    @Value("${gemini.model.embedding}")
    private String embeddingModel;

    public GeminiService() {
        this.restClient = RestClient.builder().build();
    }

    public String summarize(String code) {
        String url = baseUrl + "/" + chatModel + ":generateContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", "Summarize this Java code in 1 sentence: \n" + code)
                        ))
                )
        );

        try {
            Map response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List candidates = (List) response.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);

            return (String) firstPart.get("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating summary.";
        }
    }
    public String chat(String prompt) {
        String url = baseUrl + "/" + chatModel + ":generateContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            Map response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List candidates = (List) response.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);

            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);

            return (String) firstPart.get("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating Chat response.";
        }
    }

    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) {
        String url = baseUrl + "/" + embeddingModel + ":embedContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of(
                        "parts", List.of(
                                Map.of("text", text.replaceAll("\\s+", " "))
                        )
                )
        );

        try {
            Map response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            Map embedding = (Map) response.get("embedding");
            return (List<Double>) embedding.get("values");

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // Optional helper if you ever need float[]
    public float[] embedAsFloatArray(String text) {
        List<Double> values = embed(text);
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }
}
