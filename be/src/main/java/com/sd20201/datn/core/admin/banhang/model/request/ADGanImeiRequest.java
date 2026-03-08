package com.sd20201.datn.core.admin.banhang.model.request;

import lombok.Data;
import java.util.List;

@Data
public class ADGanImeiRequest {
    private String hoaDonChiTietId;
    private List<String> imeiIds;
}