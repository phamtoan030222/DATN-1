package com.sd20201.datn.core.client.chatMessage.chat.controller;

import com.sd20201.datn.core.client.chatMessage.chat.model.request.ChatRequest;
import com.sd20201.datn.core.client.chatMessage.chat.service.ChatServiceImpl;
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
import java.util.Map;

@RestController
@RequestMapping(MappingConstants.API_CHAT_AI)
@RequiredArgsConstructor
@CrossOrigin("*")
public class ChatController {
    private final ChatServiceImpl chatService;

    @PostMapping("/ask")
    public ResponseEntity<String> askAi(@RequestBody ChatRequest request) {
        String aiResponse = chatService.chatWithAi(request);
        return ResponseEntity.ok(aiResponse);
    }


    //Api chuyển sang gặp nhân viên khi yêu cầu
    @PostMapping("/switch-to-human")
    public ResponseEntity<?>switchToHuman(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        chatService.enableHumanSupport(sessionId);
        return ResponseEntity.ok("Đã chuyển sang chế độ nhờ nhân viên hỗ trợ");
    }

    // nhân viên trả lời
    @PostMapping("/staff/reply")
    public ResponseEntity<?> staffReply(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        String message = payload.get("message");
        String staffId = payload.get("staffId");

        chatService.staffReply(sessionId, message, staffId);
        return ResponseEntity.ok("Đã gửi tin nhắn trả lời");
    }
}
