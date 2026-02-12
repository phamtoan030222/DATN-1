package com.sd20201.datn.infrastructure.constant;

public enum ImeiStatus {
    // Trạng thái kho
    AVAILABLE,      // Sẵn sàng bán
    RESERVED,       // Đã đặt trước (chờ thanh toán)
    SOLD,                     // Đã bán
    RETURNED,        // Khách trả hàng
    DEFECTIVE,      // Sản phẩm lỗi
    WARRANTY,      // Đang bảo hành
    REPAIRING,    // Đang sửa chữa
    LOST,                 // Bị mất
    DEMO               // Máy trưng bày
}