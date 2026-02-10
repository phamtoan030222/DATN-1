package com.sd20201.datn.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sd20201.datn.entity.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_schedule")
public class WorkSchedule extends PrimaryEntity {

    // Nếu bạn không dùng account nữa thì có thể bỏ qua, nhưng cứ để cũng được
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    // 👇 QUAN TRỌNG: PHẢI THÊM VÀO ĐÂY (VÌ LỖI ĐANG Ở SHIFT)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    private LocalDate workDate;

    private String note;

    // 👇 THÊM CẢ VÀO ĐÂY NỮA CHO CHẮC CHẮN
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;
}