package com.sd20201.datn.core.client.chatMessage.chat.repository;

import com.sd20201.datn.entity.ChatMessage;
import com.sd20201.datn.repository.ChatMessageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdChatMessageRepository extends ChatMessageRepository {
    List<ChatMessage> findTop10BySessionIdOrderByCreatedDateDesc(String sessionId);
}
