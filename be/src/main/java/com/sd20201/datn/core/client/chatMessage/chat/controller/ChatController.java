package com.sd20201.datn.core.client.chatMessage.chat.controller;

import com.sd20201.datn.core.client.chatMessage.chat.model.request.ChatRequest;
import com.sd20201.datn.core.client.chatMessage.chat.service.ChatServiceImpl;
import com.sd20201.datn.core.client.chatMessage.chat.service.GeminiService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_CHAT_AI)
@RequiredArgsConstructor
@CrossOrigin("*")
public class ChatController {
    private final ChatServiceImpl chatService;
    private final GeminiService geminiService;

    @PostMapping("/ask")
    public ResponseEntity<String> askAi(@RequestBody ChatRequest request) {
        String aiResponse = chatService.chatWithAi(request);
        return ResponseEntity.ok(aiResponse);
    }

    @Autowired
    ChatLanguageModel chatLanguageModel;

    @GetMapping("/test-gemini")
    public String test() {
        return chatLanguageModel.generate("Xin chào");
    }

}
