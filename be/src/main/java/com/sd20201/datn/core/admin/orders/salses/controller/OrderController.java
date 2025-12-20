//package com.sd20201.datn.core.admin.orders.order.controller;
//
//import com.sd20201.datn.core.admin.orders.order.model.request.OrderRequest;
//import com.sd20201.datn.core.admin.orders.order.service.OrderService;
//import com.sd20201.datn.core.common.base.ResponseObject;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/admin/orders")
//@CrossOrigin(origins = "*") // Cho phép FE gọi
//public class OrderController {
//
//    @Autowired
//    private OrderService orderService;
//
//    // API: POST /api/admin/orders/create-counter
//    @PostMapping("/create-counter")
//    public ResponseObject<Object> createOrderCounter(@RequestBody OrderRequest request) {
//        return orderService.createOrderAtCounter(request);
//    }
//}