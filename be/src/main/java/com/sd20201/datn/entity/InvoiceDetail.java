package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "invoice_detail")
public class InvoiceDetail extends PrimaryEntity implements Serializable {

    @ManyToOne
    @JoinColumn(name = "id_invoice", referencedColumnName = "id")
    private Invoice invoice;

    @ManyToOne
    @JoinColumn(name = "id_imei_sold", referencedColumnName = "id")
    private IMEISold iMEISold;

    @ManyToOne
    @JoinColumn(name = "id_product_detail", referencedColumnName = "id")
    private ProductDetail productDetail;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal totalAmount;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;
}
