package com.sd20201.datn.core.admin.hoadon.model.request;

import lombok.Data;

@Data
public class ADDoiImeiRequest {
    private String hoaDonChiTietId;  // ID của InvoiceDetail
    private String oldImeiId;         // ID serial cũ cần thay
    private String newImeiId;         // ID serial mới muốn gán vào
}