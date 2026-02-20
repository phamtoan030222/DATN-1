package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient; // <--- Thêm import này
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shift")
public class Shift extends PrimaryEntity {
    @Column(unique = true)
    private String name; // Ví dụ: "Ca Sáng", "Ca Chiều"

    // Lưu giờ quy định dạng String "HH:mm:ss" (VD: "08:00:00")
    private String startTime;

    private String endTime;

    private String description;

    // 👇 Cờ báo hiệu cho Frontend biết ca này đã có lịch sử chưa
    @Transient
    private boolean hasHistory;
}