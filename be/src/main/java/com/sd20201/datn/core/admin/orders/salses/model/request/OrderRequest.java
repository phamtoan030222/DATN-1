package com.sd20201.datn.core.admin.orders.salses.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    // Thông tin chung
    private String staffId;       // Nhân viên bán
    private String customerId;    // Khách hàng
    private String voucherId;     // Mã giảm giá

    private BigDecimal totalAmount;              // Tổng tiền hàng
    private BigDecimal totalAmountAfterDecrease; // Tổng tiền sau giảm
    private BigDecimal shippingFee;              // Phí ship (nếu có)
    private String description;                  // Ghi chú

    // Thông tin người nhận (nếu có)
    private String nameReceiver;
    private String phoneReceiver;
    private String addressReceiver;

    private Integer paymentMethod; // 0: Tiền mặt, 1: CK, 2: Cả hai
    private BigDecimal cashAmount; // Tiền mặt khách đưa
    private BigDecimal transferAmount; // Tiền khách CK

    // Danh sách sản phẩm
    private List<ProductDetailOrder> products;

    // Class con định nghĩa sản phẩm trong giỏ
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetailOrder {
        private String productDetailId; // ID sản phẩm
        private Integer quantity;       // Số lượng
        private BigDecimal price;       // Giá bán thực tế
        private List<String> listImeiIds; // Danh sách ID của IMEI (nếu có)
    }
}