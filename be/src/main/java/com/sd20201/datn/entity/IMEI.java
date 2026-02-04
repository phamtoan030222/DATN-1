package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.ImeiStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "imei")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IMEI extends PrimaryEntity implements Serializable {

    @ManyToOne
    @JoinColumn(name = "id_product_detail", referencedColumnName = "id")
    private ProductDetail productDetail;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "trang_thai_imei")
    private ImeiStatus imeiStatus = ImeiStatus.AVAILABLE;

    @ManyToOne
    @JoinColumn(name = "id_invoice_detail")
    private InvoiceDetail invoiceDetail;  // Thêm quan hệ này

    @ManyToOne
    @JoinColumn(name = "id_invoice")
    private Invoice invoiceHolding;  // Hóa đơn đang giữ (giỏ hàng)

    private Long lockedAt;

    public void setSoldAt(long l) {
    }
}
