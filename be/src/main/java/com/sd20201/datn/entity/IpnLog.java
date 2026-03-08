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
@Table(name = "ipn_logs")
public class IpnLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_code", length = 50)
    private String invoiceCode;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "transaction_no", length = 50)
    private String transactionNo;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "pay_date", length = 20)
    private String payDate;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    @Column(name = "created_at")
    private Date createdAt;
}