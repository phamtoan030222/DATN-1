package com.sd20201.datn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @Column(name = "invoice_code", length = 50)
    private String invoiceCode;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "status", length = 20)
    private String status; // PENDING, SUCCESS, FAILED

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 15)
    private String customerPhone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "transaction_no", length = 50)
    private String transactionNo;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}