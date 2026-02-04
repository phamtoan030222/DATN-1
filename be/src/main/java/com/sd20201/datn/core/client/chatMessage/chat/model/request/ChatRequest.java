package com.sd20201.datn.core.client.chatMessage.chat.model.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatRequest {
    private String sessionId;
    private String message;
    private String customerId;
    private String staffId;
}
