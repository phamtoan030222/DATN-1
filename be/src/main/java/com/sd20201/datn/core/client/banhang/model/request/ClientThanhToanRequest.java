package com.sd20201.datn.core.client.banhang.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientThanhToanRequest {

    private String ten;

    private String sdt;

    private String diaChi;

    private String idNV;

    private String idHD;

    private String idPGG;

    private BigDecimal tienHang;

    private BigDecimal giamGia;

    private BigDecimal tongTien;

    private Integer check;

    private BigDecimal tienShip;

    private String phuongThucThanhToan;
}

