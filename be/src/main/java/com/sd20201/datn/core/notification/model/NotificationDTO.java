package com.sd20201.datn.core.notification.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private String id;
    private String title;
    private String description;
    private String relatedId;
    private String relatedCode;
    private String type;
    private boolean isRead;
    private Long createdAt;
}