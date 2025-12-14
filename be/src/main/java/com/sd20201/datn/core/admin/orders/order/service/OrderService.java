package com.sd20201.datn.core.admin.orders.order.service;

import com.sd20201.datn.core.admin.orders.order.model.request.OrderRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface OrderService {

    // API tạo hóa đơn tại quầy
    ResponseObject<Object> createOrderAtCounter(OrderRequest request);

    Boolean updateOrder(OrderRequest orderRequest, String id);

    Boolean deleteOrder(String id);
}