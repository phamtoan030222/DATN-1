package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification")
public class Notification extends AuditEntity {

    @Id
    @Column(length = 36, updatable = false)
    private String id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "related_id", length = 36)
    private String relatedId;   

    @Column(name = "related_code", length = 100)
    private String relatedCode;

    @Column(length = 50)
    private String type;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}