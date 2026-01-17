package com.sd20201.datn.infrastructure.constant;

public enum TypePayment {
    TIEN_MAT("Tiền mặt"),
    CHUYEN_KHOAN("Chuyển khoản"),
    THE_TIN_DUNG("Thẻ tín dụng/Thẻ ghi nợ"),
    VI_DIEN_TU("Ví điện tử"),
    TIEN_MAT_CHUYEN_KHOAN("Tiền mặt + Chuyển khoản");

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