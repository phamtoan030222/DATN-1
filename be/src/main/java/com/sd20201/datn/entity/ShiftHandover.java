package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityStatus; // Import cái này để hết lỗi Build
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shift_handover")
public class ShiftHandover extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(precision = 15, scale = 2)
    private BigDecimal initialCash;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalCashAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal realCashAmount;

    @Lob
    @Column(name = "note", length = 10000000)
    private String note;

    // 👇 QUAN TRỌNG: Map đúng cột 'status' và dùng EntityStatus có sẵn
    @Column(name = "status")
    @Enumerated(EnumType.ORDINAL)
    private EntityStatus status; // (Nếu PrimaryEntity đã có biến này thì có thể xóa dòng này đi, nhưng để đây cho chắc chắn map đúng cột)

    @Column(name = "name")
    private String name;

    @Column(name = "total_bills")
    private Integer totalBills;
}