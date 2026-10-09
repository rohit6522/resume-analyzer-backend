package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public String generateContent(String prompt) {

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        String response;
        try {
            response = restClient.post()
                    .uri(apiUrl)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Gemini API request failed", e);
            throw new ApiException(
                    "AI service is temporarily unavailable. Please try again in a moment.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            if (text == null || text.isBlank()) {
                throw new ApiException("AI returned an empty response. Please try again.",
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            return text;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            throw new ApiException("Could not read the AI response. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}