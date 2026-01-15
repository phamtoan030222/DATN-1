package com.sd20201.datn.core.admin.invoices.invoice.model.request;

import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdInvoiceCreUpdReqest {
    private String idStaff;
    private String idVoucher;
    private String idCustomer;
    private String idShippingMethod;

    private String code;

    private String invoiceType;

    @Enumerated(EnumType.ORDINAL)
    private TypeInvoice typeInvoice;

    private BigDecimal shippingFee;

    private BigDecimal totalAmount;

    private BigDecimal totalAmountAfterDecrease;

    private String nameReceiver;

    private String addressReceiver;

    private String phoneReceiver;

    private Long paymentDate;

    private String description;
}
