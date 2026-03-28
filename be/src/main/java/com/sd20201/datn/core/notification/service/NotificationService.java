package com.sd20201.datn.core.notification.service;

import com.sd20201.datn.core.notification.model.NotificationDTO;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.Notification;
import com.sd20201.datn.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public void sendNewOrderNotification(Invoice invoice) {
        try {
            // Ưu tiên: customer name → nameReceiver → "Khách lẻ"
            String customerName = Optional.ofNullable(invoice.getCustomer())
                    .map(c -> c.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .orElseGet(() ->
                            Optional.ofNullable(invoice.getNameReceiver())
                                    .filter(name -> !name.isBlank())
                                    .orElse("Khách lẻ")
                    );

            String title = "Đơn hàng mới";
            String description = customerName + " vừa đặt đơn " + invoice.getCode();

            // 1. Lưu DB
            Notification notification = Notification.builder()
                    .title(title)
                    .description(description)
                    .relatedId(invoice.getId())
                    .relatedCode(invoice.getCode())
                    .type("ORDER_NEW")
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);

            // 2. Broadcast WebSocket → tất cả admin đang online nhận được
            NotificationDTO dto = NotificationDTO.builder()
                    .id(notification.getId())
                    .title(title)
                    .description(description)
                    .relatedId(invoice.getId())
                    .relatedCode(invoice.getCode())
                    .type("ORDER_NEW")
                    .isRead(false)
                    .createdAt(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/admin-notifications", dto);
            log.info("[Notification] Gửi thông báo đơn mới: {}", invoice.getCode());

        } catch (Exception e) {
            // Không throw ra ngoài — lỗi notification không được chặn luồng đặt hàng
            log.error("[Notification] Lỗi gửi thông báo: ", e);
        }
    }


    // Service
    public List<NotificationDTO> getAll() {
        return notificationRepository.findAllByIsReadFalseOrderByCreatedDateDesc()
                .stream()
                .map(n -> NotificationDTO.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .description(n.getDescription())
                        .relatedId(n.getRelatedId())
                        .relatedCode(n.getRelatedCode())
                        .type(n.getType())
                        .isRead(n.isRead())
                        .createdAt(n.getCreatedDate())
                        .build())
                .collect(Collectors.toList());
    }

    public void sendCancelOrderNotification(Invoice invoice) {
        try {
            String customerName = Optional.ofNullable(invoice.getCustomer())
                    .map(c -> c.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .orElseGet(() ->
                            Optional.ofNullable(invoice.getNameReceiver())
                                    .filter(name -> !name.isBlank())
                                    .orElse("Khách lẻ")
                    );

            String title = "Đơn hàng bị hủy";
            String description = customerName + " vừa hủy đơn " + invoice.getCode();

            Notification notification = Notification.builder()
                    .title(title)
                    .description(description)
                    .relatedId(invoice.getId())
                    .relatedCode(invoice.getCode())
                    .type("ORDER_CANCEL")
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);

            NotificationDTO dto = NotificationDTO.builder()
                    .id(notification.getId())
                    .title(title)
                    .description(description)
                    .relatedId(invoice.getId())
                    .relatedCode(invoice.getCode())
                    .type("ORDER_CANCEL")
                    .isRead(false)
                    .createdAt(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/admin-notifications", dto);
            log.info("[Notification] Gửi thông báo hủy đơn: {}", invoice.getCode());

        } catch (Exception e) {
            log.error("[Notification] Lỗi gửi thông báo hủy: ", e);
        }
    }

    @Transactional
    public void markAsRead(String id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead();
    }
}