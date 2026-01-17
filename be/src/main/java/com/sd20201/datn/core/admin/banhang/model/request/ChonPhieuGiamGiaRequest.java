package com.sd20201.datn.core.admin.banhang.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChonPhieuGiamGiaRequest {

    private String idKH;

    private BigDecimal tongTien;

}
