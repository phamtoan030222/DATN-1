package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.*;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_shift", referencedColumnName = "id")
    // Đặt tên cột là id_shift cho giống các cột id_staff bên trên
    private ShiftHandover shiftHandover;

    @Enumerated(EnumType.ORDINAL)
    private TypeInvoice typeInvoice;

    private BigDecimal shippingFee; // tien ship

    private BigDecimal totalAmount; // tong tien

    private BigDecimal totalAmountAfterDecrease; //tien hang

    @Column(length = EntityProperties.LENGTH_NAME)
    private String nameReceiver;

    @Column(length = EntityProperties.LENGTH_CONTENT)
    private String addressReceiver;

    @Column(length = 15)
    private String phoneReceiver;

    private String email;

    private Long paymentDate;

    private String description;

    @Column(name = "trang_thai_hoa_don")
    private EntityTrangThaiHoaDon entityTrangThaiHoaDon;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_thanh_toan")
    private TrangThaiThanhToan trangThaiThanhToan = TrangThaiThanhToan.CHUA_THANH_TOAN;

    private String ten;

    private String sdt;

    @Column(name = "phuong_thuc_thanh_toan")
    private TypePayment typePayment;

    private String transactionId;

    private String bankCode;

    private Boolean daHoanPhi;

    private BigDecimal hoanPhi;

    private BigDecimal giamGia;
}