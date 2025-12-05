package com.sd20201.datn.core.admin.orders.order.model.request;

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

    // Thông tin chung của đơn hàng
    private String staffId;       // ID nhân viên bán hàng
    private String customerId;    // ID khách hàng (có thể null nếu khách lẻ)
    private String voucherId;     // ID phiếu giảm giá (có thể null)

    private BigDecimal totalAmount;             // Tổng tiền hàng
    private BigDecimal totalAmountAfterDecrease; // Tổng tiền sau khi giảm giá
    private BigDecimal shippingFee;             // Phí vận chuyển (thường = 0 tại quầy)

    private String description;   // Ghi chú đơn hàng
    private String nameReceiver;  // Tên người nhận (nếu có giao)
    private String phoneReceiver; // SĐT người nhận
    private String addressReceiver; // Địa chỉ nhận

    private Integer paymentMethod; // Phương thức thanh toán (0: Tiền mặt, 1: Chuyển khoản, 2: Cả hai)
    private BigDecimal cashAmount; // Số tiền mặt khách đưa
    private BigDecimal transferAmount; // Số tiền chuyển khoản

    private Integer typeInvoice;   // 1: Tại quầy, 2: Online

    // Danh sách sản phẩm muốn mua
    private List<ProductDetailOrder> products;

    /**
     * Inner class để định nghĩa chi tiết từng sản phẩm trong đơn hàng
     * Bạn chưa có class này nên chúng ta định nghĩa luôn ở đây (static class)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetailOrder {
        private String productDetailId; // ID chi tiết sản phẩm
        private Integer quantity;       // Số lượng mua
        private BigDecimal price;       // Giá bán tại thời điểm mua

        // Danh sách ID của các IMEI được chọn cho sản phẩm này (quan trọng cho đồ điện tử)
        private List<String> listImeiIds;
    }
}