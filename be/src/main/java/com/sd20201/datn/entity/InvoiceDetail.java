package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "invoice_detail")
public class InvoiceDetail extends PrimaryEntity implements Serializable {


        @ManyToOne
        @JoinColumn(name = "id_invoice")
        private Invoice invoice;

        @ManyToOne
        @JoinColumn(name = "id_product_detail")
        private ProductDetail productDetail;

        @OneToMany(mappedBy = "invoiceDetail")
        private List<IMEI> imeis;  // Danh sách IMEI trong chi tiết hóa đơn

        private BigDecimal price;
        private Integer quantity;  // Số lượng = số IMEI
        private BigDecimal totalAmount;
}
