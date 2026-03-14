package com.sd20201.datn.core.admin.hoadon.model.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ADHoanPhiRequest {

    private String maHoaDon;

    private String idNhanVien;

    private BigDecimal hoanPhi;
}
