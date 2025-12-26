//package com.sd20201.datn.core.admin.orders.order.service.impl;
//
//import com.sd20201.datn.core.admin.orders.order.model.request.OrderRequest;
//import com.sd20201.datn.core.admin.orders.order.service.OrderService;
//import com.sd20201.datn.core.common.base.ResponseObject;
//import com.sd20201.datn.entity.IMEI;
//import com.sd20201.datn.entity.IMEISold;
//import com.sd20201.datn.entity.Invoice;
//import com.sd20201.datn.entity.InvoiceDetail;
//import com.sd20201.datn.entity.ProductDetail;
//import com.sd20201.datn.infrastructure.constant.EntityStatus;
//import com.sd20201.datn.infrastructure.constant.TypeInvoice;
//import com.sd20201.datn.repository.CustomerRepository;
//import com.sd20201.datn.repository.IMEIRepository;
//import com.sd20201.datn.repository.IMEISoldRepository;
//import com.sd20201.datn.repository.InvoiceDetailRepository;
//import com.sd20201.datn.repository.InvoiceRepository;
//import com.sd20201.datn.repository.ProductDetailRepository;
//import com.sd20201.datn.repository.StaffRepository;
//import com.sd20201.datn.repository.VoucherRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class OrderServiceImpl implements OrderService {
//
//    private final InvoiceRepository invoiceRepository;
//    private final InvoiceDetailRepository invoiceDetailRepository;
//    private final ProductDetailRepository productDetailRepository;
//    private final IMEIRepository imeiRepository;
//    private final IMEISoldRepository imeiSoldRepository;
//    private final StaffRepository staffRepository;
//    private final CustomerRepository customerRepository;
//    private final VoucherRepository voucherRepository;
//
//    @Override
//    @Transactional
//    public ResponseObject<Object> createOrderAtCounter(OrderRequest request) {
//        // 1. Khởi tạo hóa đơn (Invoice)
//        Invoice invoice = new Invoice();
//
//        // --- Gán Staff (Nhân viên) ---
//        if (request.getStaffId() != null) {
//            invoice.setStaff(staffRepository.findById(request.getStaffId()).orElse(null));
//        }
//
//        // --- Gán Customer (Khách hàng) ---
//        if (request.getCustomerId() != null) {
//            invoice.setCustomer(customerRepository.findById(request.getCustomerId()).orElse(null));
//        }
//
//        // --- Gán Voucher ---
//        if (request.getVoucherId() != null) {
//            invoice.setVoucher(voucherRepository.findById(request.getVoucherId()).orElse(null));
//        }
//
//        // --- Gán thông tin tiền ---
//        invoice.setTotalAmount(request.getTotalAmount());
//        invoice.setTotalAmountAfterDecrease(request.getTotalAmountAfterDecrease());
//        invoice.setShippingFee(BigDecimal.ZERO);
//
//        // --- Cấu hình trạng thái và loại hóa đơn ---
//        invoice.setTypeInvoice(TypeInvoice.OFFLINE);
//        invoice.setPaymentDate(System.currentTimeMillis());
//        invoice.setDescription(request.getDescription());
//
//        // --- Thông tin người nhận (nếu có) ---
//        invoice.setNameReceiver(request.getNameReceiver());
//        invoice.setPhoneReceiver(request.getPhoneReceiver());
//        invoice.setAddressReceiver(request.getAddressReceiver());
//
//        // Set status ACTIVE (Sử dụng Enum thay vì số)
//        invoice.setStatus(EntityStatus.ACTIVE);
//
//        // Lưu Hóa đơn
//        Invoice savedInvoice = invoiceRepository.save(invoice);
//
//        // 2. Xử lý chi tiết hóa đơn (InvoiceDetail)
//        List<InvoiceDetail> detailsToSave = new ArrayList<>();
//
//        if (request.getProducts() != null) {
//            for (OrderRequest.ProductDetailOrder item : request.getProducts()) {
//                // Tìm sản phẩm chi tiết
//                ProductDetail productDetail = productDetailRepository.findById(item.getProductDetailId()).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + item.getProductDetailId()));
//
//                // --- TRƯỜNG HỢP CÓ IMEI (Điện thoại, Laptop...) ---
//                if (item.getListImeiIds() != null && !item.getListImeiIds().isEmpty()) {
//                    for (String imeiId : item.getListImeiIds()) {
//                        // Tìm IMEI
//                        IMEI imei = imeiRepository.findById(imeiId).orElseThrow(() -> new RuntimeException("IMEI không tồn tại"));
//
//                        // Validate status (Kiểm tra xem IMEI có đang ACTIVE không)
//                        if (imei.getStatus() != EntityStatus.ACTIVE) {
//                            throw new RuntimeException("IMEI đã bán hoặc không khả dụng!");
//                        }
//
//                        // Cập nhật IMEI -> Đã bán (INACTIVE)
//                        imei.setStatus(EntityStatus.INACTIVE);
//                        imeiRepository.save(imei);
//
//                        // Tạo bản ghi IMEISold (Lưu vết bán)
//                        IMEISold imeiSold = new IMEISold();
//                        imeiSold.setDateSold(System.currentTimeMillis());
//                        imeiSold.setDescription("Bán cho đơn hàng: " + savedInvoice.getId());
//
//                        // [QUAN TRỌNG] Entity IMEISold của bạn thiếu trường "imei" nên dòng này bị comment
//                        // imeiSold.setImei(imei);
//
//                        IMEISold savedImeiSold = imeiSoldRepository.save(imeiSold);
//
//                        // Tạo InvoiceDetail
//                        InvoiceDetail detail = new InvoiceDetail();
//                        detail.setInvoice(savedInvoice);
//                        detail.setProductDetail(productDetail);
//
//                        // Link tới bảng IMEISold (Lưu ý tên getter/setter của Lombok)
//                        detail.setIMEISold(savedImeiSold);
//
//                        detail.setQuantity(1);
//                        detail.setPrice(item.getPrice());
//                        detail.setTotalAmount(item.getPrice());
//                        detail.setStatus(EntityStatus.ACTIVE);
//
//                        detailsToSave.add(detail);
//                    }
//
//                    // [QUAN TRỌNG] Entity ProductDetail thiếu trường "quantity" nên logic trừ kho bị comment
//                    // productDetail.setQuantity(productDetail.getQuantity() - item.getListImeiIds().size());
//                    // productDetailRepository.save(productDetail);
//
//                }
//                // --- TRƯỜNG HỢP SẢN PHẨM KHÔNG IMEI (Phụ kiện...) ---
//                else {
//                    // Check tồn kho (Bị comment do thiếu field quantity)
//                    // if (productDetail.getQuantity() < item.getQuantity()) throw...
//
//                    InvoiceDetail detail = new InvoiceDetail();
//                    detail.setInvoice(savedInvoice);
//                    detail.setProductDetail(productDetail);
//
//                    detail.setQuantity(item.getQuantity());
//                    detail.setPrice(item.getPrice());
//                    detail.setTotalAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
//                    detail.setStatus(EntityStatus.ACTIVE);
//
//                    detailsToSave.add(detail);
//
//                    // Trừ tồn kho (Bị comment do thiếu field quantity)
//                    // productDetail.setQuantity(productDetail.getQuantity() - item.getQuantity());
//                    // productDetailRepository.save(productDetail);
//                }
//            }
//        }
//
//        // Lưu tất cả chi tiết
//        invoiceDetailRepository.saveAll(detailsToSave);
//
//        return new ResponseObject<>(savedInvoice, HttpStatus.CREATED, "Thanh toán thành công");
//    }
//
//    @Override
//    public Boolean updateOrder(OrderRequest orderRequest, String id) {
//        return null;
//    }
//
//    @Override
//    public Boolean deleteOrder(String id) {
//        return null;
//    }
//}