package com.sd20201.datn.core.admin.banhang.model.response;

import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class ApplicableVoucherResponse {
    private String voucherId;
    private String code;
    private TypeVoucher typeVoucher;
    private BigDecimal discountValue;
    private BigDecimal maxValue;

    private BigDecimal dieuKien;
    private BigDecimal giamGiaThucTe;
    private BigDecimal tongTienSauGiam;
}
