package com.sd20201.datn.core.client.chatMessage.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

@Service
public class GeminiService {
    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String callGemini(String text) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part = new HashMap<>();
        part.put("text", text);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // ✅ Retry tối đa 3 lần khi gặp lỗi 503
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

                if (response != null && response.containsKey("candidates")) {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map<String, Object> contentRes = (Map<String, Object>) candidates.get(0).get("content");
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentRes.get("parts");
                        return (String) parts.get(0).get("text");
                    }
                }
                return "AI không phản hồi (Empty response).";

            } catch (HttpServerErrorException.ServiceUnavailable e) {
                // ✅ Chỉ retry khi lỗi 503 (Gemini quá tải)
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000L * attempt); // lần 1: 2s, lần 2: 4s, lần 3: dừng
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    return "AI đang quá tải, vui lòng thử lại sau ít phút.";
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Lỗi kết nối Gemini: " + e.getMessage();
            }
        }

        return "AI đang quá tải, vui lòng thử lại sau ít phút.";
    }
}