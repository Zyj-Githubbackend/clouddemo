package org.example.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.example.dto.AIGenerateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.api.url:}")
    private String apiUrl;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate;

    public AIService(RestTemplateBuilder restTemplateBuilder,
                     @Value("${ai.api.connect-timeout:3s}") Duration connectTimeout,
                     @Value("${ai.api.read-timeout:10s}") Duration readTimeout) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    @Retry(name = "aiGenerator", fallbackMethod = "generateActivityDescriptionFallback")
    @CircuitBreaker(name = "aiGenerator")
    @Bulkhead(name = "aiGenerator", type = Bulkhead.Type.SEMAPHORE)
    public String generateActivityDescription(AIGenerateRequest request) {
        if (isBlank(apiUrl) || isBlank(apiKey)) {
            log.warn("ai generation fallback because api configuration is missing category={} location={}",
                    request.getCategory(), request.getLocation());
            return generateFallbackDescription(request);
        }

        try {
            String prompt = buildPrompt(request);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);

            String generatedContent = extractGeneratedContent(response);
            if (!isBlank(generatedContent)) {
                log.info("ai generation succeeded category={} location={} model={}",
                        request.getCategory(), request.getLocation(), model);
                return generatedContent.trim();
            }
            throw new IllegalStateException("AI response does not contain usable choices");
        } catch (RuntimeException ex) {
            log.warn("ai generation attempt failed category={} location={} model={} error={}",
                    request.getCategory(), request.getLocation(), model, ex.getMessage());
            throw ex;
        }
    }

    private String generateActivityDescriptionFallback(AIGenerateRequest request, Throwable throwable) {
        log.error("ai generation fallback after resilience category={} location={} model={} error={}",
                request.getCategory(), request.getLocation(), model, throwable.getMessage(), throwable);
        return generateFallbackDescription(request);
    }

    private String generateFallbackDescription(AIGenerateRequest request) {
        String location = fallbackValue(request.getLocation(), "\u5f85\u786e\u8ba4\u5730\u70b9");
        String category = fallbackValue(request.getCategory(), "\u6821\u56ed\u5fd7\u613f\u670d\u52a1");
        String keywords = fallbackValue(request.getKeywords(), "\u73b0\u573a\u534f\u52a9\u3001\u79e9\u5e8f\u5f15\u5bfc\u3001\u6696\u5fc3\u670d\u52a1");
        String hours = request.getVolunteerHours() != null
                ? request.getVolunteerHours().stripTrailingZeros().toPlainString()
                : "\u5f85\u786e\u8ba4";
        return String.format(
                "\u3010\u7cfb\u7edf\u515c\u5e95\u6587\u6848\u3011\u672c\u6b21AI\u6587\u6848\u751f\u6210\u6682\u65f6\u672a\u5b8c\u6210\uff0c\u5df2\u4e3a\u4f60\u751f\u6210\u53ef\u76f4\u63a5\u4fee\u6539\u7684\u6d3b\u52a8\u62db\u52df\u6587\u6848\u3002%n%n" +
                        "\u73b0\u9762\u5411\u540c\u5b66\u62db\u52df\u201c%s\u201d\u7c7b\u5fd7\u613f\u670d\u52a1\u53c2\u4e0e\u8005\u3002\u6d3b\u52a8\u5730\u70b9\u4e3a%s\uff0c\u670d\u52a1\u5173\u952e\u8bcd\u5305\u62ec%s\u3002\u672c\u6b21\u6d3b\u52a8\u8ba1\u5165%s\u5c0f\u65f6\u5fd7\u613f\u65f6\u957f\u3002%n%n" +
                        "\u6b22\u8fce\u6709\u8d23\u4efb\u5fc3\u3001\u613f\u610f\u4e3b\u52a8\u534f\u4f5c\u7684\u540c\u5b66\u62a5\u540d\u53c2\u4e0e\u3002\u8bf7\u5927\u5bb6\u6309\u65f6\u5230\u573a\uff0c\u914d\u5408\u73b0\u573a\u5de5\u4f5c\u5b89\u6392\uff0c\u4ee5\u70ed\u5fc3\u3001\u8010\u5fc3\u548c\u7ec6\u5fc3\u5b8c\u6210\u670d\u52a1\u4efb\u52a1\uff0c\u5171\u540c\u8425\u9020\u6709\u5e8f\u3001\u53cb\u5584\u3001\u6e29\u6696\u7684\u6821\u56ed\u516c\u76ca\u6c1b\u56f4\u3002",
                category,
                location,
                keywords,
                hours
        );
    }

    private String buildPrompt(AIGenerateRequest request) {
        return String.format(
                "Please write a warm Chinese recruitment description for a campus volunteer activity in 200-300 Chinese characters.%n" +
                        "Location: %s%n" +
                        "Category: %s%n" +
                        "Keywords: %s%n" +
                        "Volunteer hours: %s%n" +
                        "Requirements:%n" +
                        "1. Highlight the meaning and value of the activity.%n" +
                        "2. Describe the main volunteer work.%n" +
                        "3. Mention basic expectations for volunteers.%n" +
                        "4. Clearly mention the volunteer hours.%n" +
                        "5. The final answer must be in Chinese only.",
                fallbackValue(request.getLocation(), "TBD"),
                fallbackValue(request.getCategory(), "Campus volunteer service"),
                fallbackValue(request.getKeywords(), "service, teamwork"),
                request.getVolunteerHours() != null ? request.getVolunteerHours().toString() : "TBD"
        );
    }

    private String extractGeneratedContent(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object choicesObject = response.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return null;
        }
        Object messageObject = choice.get("message");
        if (!(messageObject instanceof Map<?, ?> message)) {
            return null;
        }
        Object content = message.get("content");
        return Objects.toString(content, null);
    }

    private String fallbackValue(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
