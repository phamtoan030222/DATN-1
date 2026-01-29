package com.sd20201.datn.core.client.banhang.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class ClientBetterVoucherResponse {

    private String voucherId;
    private String code;

    private BigDecimal dieuKien;
    private BigDecimal canMuaThem;

    private BigDecimal giamGiaMoi;
    private BigDecimal giamThem;

    private BigDecimal hieuQua; // %
}
