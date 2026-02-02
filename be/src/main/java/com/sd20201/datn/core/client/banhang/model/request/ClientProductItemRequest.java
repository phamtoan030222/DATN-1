package com.sd20201.datn.core.client.banhang.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientProductItemRequest {

    // ID của sản phẩm chi tiết (ProductDetail)
    private String productDetailId;

    // Số lượng khách mua
    private Integer quantity;

    // Giá bán tại thời điểm đặt hàng (Để lưu vào db cho chính xác, phòng khi giá sp thay đổi sau này)
    private BigDecimal price;

}