package org.example.service;

import org.example.dto.AIGenerateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.api.url:}")
    private String apiUrl;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateActivityDescription(AIGenerateRequest request) {
        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            log.warn("ai generation fallback because api configuration is missing category={} location={}",
                    request.getCategory(), request.getLocation());
            return generateFallbackDescription(request);
        }

        try {
            String prompt = String.format(
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
                            "5. The final answer must be in Chinese.",
                    request.getLocation(),
                    request.getCategory(),
                    request.getKeywords(),
                    request.getVolunteerHours() != null ? request.getVolunteerHours().toString() : "TBD"
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            requestBody.put("max_tokens", 500);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    log.info("ai generation succeeded category={} location={} model={}",
                            request.getCategory(), request.getLocation(), model);
                    return (String) message.get("content");
                }
            }

            log.warn("ai generation returned empty choices, using fallback category={} location={}",
                    request.getCategory(), request.getLocation());
            return generateFallbackDescription(request);

        } catch (Exception e) {
            log.error("ai generation failed category={} location={} model={}",
                    request.getCategory(), request.getLocation(), model, e);
            return generateFallbackDescription(request);
        }
    }

    private String generateFallbackDescription(AIGenerateRequest request) {
        String hours = request.getVolunteerHours() != null ? request.getVolunteerHours().toString() : "TBD";
        return String.format(
                "Campus volunteer activity recruitment is now open.%n%n" +
                        "Location: %s%n" +
                        "Category: %s%n" +
                        "Keywords: %s%n" +
                        "Volunteer hours: %s%n%n" +
                        "We welcome caring and responsible students to join this meaningful volunteer activity. " +
                        "You will support the event through practical action, teamwork, and service spirit while gaining valuable experience.",
                request.getLocation(),
                request.getCategory(),
                request.getKeywords(),
                hours
        );
    }
}
