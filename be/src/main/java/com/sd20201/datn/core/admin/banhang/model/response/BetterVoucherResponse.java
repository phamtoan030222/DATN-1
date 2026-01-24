package com.sd20201.datn.core.admin.banhang.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class BetterVoucherResponse {

    private String voucherId;
    private String code;

    private BigDecimal dieuKien;
    private BigDecimal canMuaThem;

    private BigDecimal giamGiaMoi;
    private BigDecimal giamThem;

    private BigDecimal hieuQua; // %
}
