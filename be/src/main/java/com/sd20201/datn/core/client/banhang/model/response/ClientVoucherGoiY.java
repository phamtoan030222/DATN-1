package com.sd20201.datn.core.client.banhang.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientVoucherGoiY {
    private String maVoucher;
    private String tenVoucher;
    private BigDecimal tongTienCanDat;
    private BigDecimal giamGiaMoi;
    private BigDecimal canMuaThem;
    private BigDecimal giamThem;
    private BigDecimal hieuQua; // %
    private String moTa;
    private String icon; // icon hiển thị

    public String getThongBaoGoiY() {
        return String.format(
                "💰 Mua thêm <b>%s</b> để nhận voucher <b>%s</b><br>" +
                        "🎁 Giảm ngay <b>%s</b> (hiệu quả <b>%.0f%%</b>)",
                formatCurrency(canMuaThem),
                tenVoucher,
                formatCurrency(giamGiaMoi),
                hieuQua.doubleValue()
        );
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0fđ", amount);
    }
}