package com.sd20201.datn.core.admin.banhang.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ADThemSanPhamRequest {


    private String invoiceId;
    private String productDetailId;
    private List<String> imeiIds;



}
