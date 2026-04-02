package org.example.service;

import org.example.dto.AIGenerateRequest;
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
    
    @Value("${ai.api.url:}")
    private String apiUrl;
    
    @Value("${ai.api.key:}")
    private String apiKey;
    
    @Value("${ai.api.model:deepseek-chat}")
    private String model;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public String generateActivityDescription(AIGenerateRequest request) {
        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            return generateFallbackDescription(request);
        }
        
        try {
            String prompt = String.format(
                "请为校园志愿活动生成一段富有号召力的招募文案（200-300字）。\n" +
                "活动地点：%s\n" +
                "活动类型：%s\n" +
                "关键词：%s\n" +
                "志愿时长：%s 小时\n" +
                "要求：\n" +
                "1. 突出活动意义和价值\n" +
                "2. 描述具体工作内容\n" +
                "3. 说明对志愿者的基本要求\n" +
                "4. 明确说明志愿时长为 %s 小时\n" +
                "5. 语言亲切、富有感染力",
                request.getLocation(), request.getCategory(), request.getKeywords(),
                request.getVolunteerHours() != null ? request.getVolunteerHours().toString() : "待定",
                request.getVolunteerHours() != null ? request.getVolunteerHours().toString() : "待定"
            );
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
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
                    return (String) message.get("content");
                }
            }
            
            return generateFallbackDescription(request);
            
        } catch (Exception e) {
            e.printStackTrace();
            return generateFallbackDescription(request);
        }
    }
    
    private String generateFallbackDescription(AIGenerateRequest request) {
        String hours = request.getVolunteerHours() != null ? request.getVolunteerHours().toString() : "待定";
        return String.format(
            "【%s】志愿服务活动火热招募中！\n\n" +
            "活动地点：%s\n" +
            "关键词：%s\n" +
            "志愿时长：%s 小时\n\n" +
            "这是一次非常有意义的志愿服务活动，我们诚邀热心公益、乐于奉献的同学加入我们的团队。" +
            "在这里，你将有机会用实际行动践行志愿精神，在服务他人的同时提升自我、收获成长。\n\n" +
            "我们期待具有责任心、团队协作精神和良好沟通能力的志愿者。" +
            "无论你是否有志愿服务经验，只要你有一颗热忱的心，我们都热烈欢迎！\n\n" +
            "让我们一起用爱心和行动，为校园、为社会贡献青春力量！",
            request.getCategory(), request.getLocation(), request.getKeywords(), hours
        );
    }
}
