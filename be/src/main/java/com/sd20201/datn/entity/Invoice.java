package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityProperties;
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "invoice")
public class Invoice extends PrimaryEntity implements Serializable {

    @ManyToOne
    @JoinColumn(name = "id_staff", referencedColumnName = "id")
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "id_voucher", referencedColumnName = "id")
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "id_customer", referencedColumnName = "id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "id_shipping_method", referencedColumnName = "id")
    private ShippingMethod shippingMethod;

    @Enumerated(EnumType.ORDINAL)
    private TypeInvoice typeInvoice;

    private BigDecimal shippingFee;

    private BigDecimal totalAmount;

    private BigDecimal totalAmountAfterDecrease;

    @Column(length = EntityProperties.LENGTH_NAME)
    private String nameReceiver;

    @Column(length = EntityProperties.LENGTH_CONTENT)
    private String addressReceiver;

    @Column(length = 15)
    private String phoneReceiver;

    private Long paymentDate;

    private String description;
}
