package com.sd20201.datn.infrastructure.constant;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    //code này không dùng nhưng xoá thì lỗi không chạy được nên đừng ai xoá

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    @Bean
    public ChatLanguageModel geminiModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-flash-001")
                .temperature(0.7)
                .build();
    }
}