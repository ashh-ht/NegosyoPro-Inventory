package com.inventory.negosyopro.model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AiTest {
    public static void main(String[] args) {
        String model = "qwen3.5:4b";

        String prompt = """
            You are a business report assistant.

            Analyze the following weekly sales data:

            Current week revenue: PHP 15,000
            Previous week revenue: PHP 12,000
            Top-selling product: Wireless Mouse

            Write a concise report containing:
            1. A summary of the results.
            2. Key observations based only on the supplied data.
            3. Recommendations, clearly labeled as suggestions.

            Do not invent missing information.
            Use PHP as the currency.
            """;

        try {
            String aiResponse = askOllama(model, prompt);

            System.out.println("\n===== AI BUSINESS REPORT =====\n");
            System.out.println(aiResponse);
            System.out.println("\n==============================");

        } catch (Exception e) {
            System.err.println("Could not get a response from Ollama.");
            System.err.println("Details: " + e.getMessage());
        }
    }

    public static String askOllama(String model, String prompt)throws Exception {

        // Build the JSON request.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode requestBody = mapper.createObjectNode();

        requestBody.put("model", model);
        requestBody.put("stream", false);
        requestBody.put("think", false);

        ObjectNode message = requestBody
                .putArray("messages")
                .addObject();

        message.put("role", "user");
        message.put("content", prompt);

        String jsonRequest = mapper.writeValueAsString(requestBody);

        // Send the request to the local Ollama API.
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .timeout(Duration.ofMinutes(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // Check whether Ollama returned a successful HTTP response.
        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Ollama returned HTTP "
                    + response.statusCode()
                    + ": " + response.body()
            );
        }

        // Parse the JSON response and extract only the AI's text.
        JsonNode root = mapper.readTree(response.body());

        String content = root
                .path("message")
                .path("content")
                .asText("");

        if (content.isBlank()) {
            throw new RuntimeException(
                    "Ollama returned an empty response."
            );
        }

        return content;
    }
}