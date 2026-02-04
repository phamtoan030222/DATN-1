package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="chat_message")
public class ChatMessage extends PrimaryEntity {

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "content", length = 65535)
    private String content;

    @Column(name = "sender_role")
    private String senderRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_customer")
    private Customer customer;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_staff")
    private Staff staff;

    @Column(name = "is_ai_replied")
    private Boolean isAiReplied = false;

}
