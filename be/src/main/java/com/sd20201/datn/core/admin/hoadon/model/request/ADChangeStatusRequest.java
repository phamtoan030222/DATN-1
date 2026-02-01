package com.sd20201.datn.core.admin.hoadon.model.request;

import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import lombok.*;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ADChangeStatusRequest {
    private String maHoaDon;               // Mã hóa đơn
    private EntityTrangThaiHoaDon statusTrangThaiHoaDon; // Trạng thái mới
    private String note;                   // Ghi chú (lý do)
    private String nhanVien;                // Nhân viên thực hiện (có thể để null, hệ thống tự lấy)
}