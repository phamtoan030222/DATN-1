package com.sd20201.datn.infrastructure.constant;

public enum TypePayment {
    TIEN_MAT("Tiền mặt"),
    VNPAY("VNPay"),
    CHUYEN_KHOAN("Chuyển khoản"),
    MOMO("Momo"),
    ZALOPAY("Zalo Pay"),
    TIEN_MAT_CHUYEN_KHOAN("Tiền mặt + Chuyển khoản"),
    VIETQR("Việt QR");

    private final String description;

    TypePayment(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static TypePayment fromValue(String value) {
        for (TypePayment type : TypePayment.values()) {
            if (type.name().equalsIgnoreCase(value) ||
                    type.getDescription().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + value);
    }
}