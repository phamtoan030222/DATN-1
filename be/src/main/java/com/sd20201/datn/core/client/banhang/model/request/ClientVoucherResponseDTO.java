package com.sd20201.datn.core.client.banhang.model.request;

import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Data
@NoArgsConstructor
public class ClientVoucherResponseDTO {
    private String id;
    private String code;
    private String name;
    private TypeVoucher typeVoucher;
    private BigDecimal discountValue;
    private BigDecimal maxValue;
    private BigDecimal conditions;
    private BigDecimal actualDiscount;
    private Long startDate;
    private Long endDate;
    private Integer remainingQuantity;
    private String additionalMessage;

    public ClientVoucherResponseDTO(String id, String code, String name, TypeVoucher typeVoucher,
                                    BigDecimal discountValue, BigDecimal maxValue, BigDecimal conditions,
                                    BigDecimal actualDiscount, Long startDate, Long endDate,
                                    Integer remainingQuantity) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.typeVoucher = typeVoucher;
        this.discountValue = discountValue;
        this.maxValue = maxValue;
        this.conditions = conditions;
        this.actualDiscount = actualDiscount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.remainingQuantity = remainingQuantity;
        this.additionalMessage = null;
    }

    // Constructor đầy đủ với additionalMessage
    public ClientVoucherResponseDTO(String id, String code, String name, TypeVoucher typeVoucher,
                                    BigDecimal discountValue, BigDecimal maxValue, BigDecimal conditions,
                                    BigDecimal actualDiscount, Long startDate, Long endDate,
                                    Integer remainingQuantity, String additionalMessage) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.typeVoucher = typeVoucher;
        this.discountValue = discountValue;
        this.maxValue = maxValue;
        this.conditions = conditions;
        this.actualDiscount = actualDiscount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.remainingQuantity = remainingQuantity;
        this.additionalMessage = additionalMessage;
    }

    public String getFormattedActualDiscount() {
        return formatCurrency(actualDiscount);
    }

    public String getFormattedConditions() {
        return conditions != null ? formatCurrency(conditions) : "Không có điều kiện";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        try {
            return NumberFormat.getCurrencyInstance(new Locale("vi", "VN"))
                    .format(amount);
        } catch (Exception e) {
            return amount.toString() + " ₫";
        }
    }

    // Thêm getter để kiểm tra null
    public BigDecimal getActualDiscount() {
        return actualDiscount != null ? actualDiscount : BigDecimal.ZERO;
    }

    public String getCode() {
        return code != null ? code : "";
    }

    public String getName() {
        return name != null ? name : "";
    }
}