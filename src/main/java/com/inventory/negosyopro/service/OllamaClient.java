package com.inventory.negosyopro.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Sends one prompt to a local Ollama server and returns the text of the answer.
public class OllamaClient {
    public static final String DEFAULT_MODEL = "qwen3.5:4b";
    public static final String DEFAULT_URL = "http://localhost:11434/api/chat";

    private final String model;
    private final String url;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaClient() {
        this(DEFAULT_MODEL, DEFAULT_URL);
    }

    public OllamaClient(String model, String url) {
        this.model = model;
        this.url = url;
    }

    public String ask(String prompt) throws IOException, InterruptedException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        body.put("think", false); // no thinking text, only the answer
        ObjectNode message = body.putArray("messages").addObject();
        message.put("role", "user");
        message.put("content", prompt);

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        String content = root.path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IOException("Ollama returned an empty response.");
        }
        return content.trim();
    }
}
