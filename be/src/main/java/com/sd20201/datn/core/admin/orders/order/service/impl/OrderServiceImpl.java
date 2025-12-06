package com.sd20201.datn.core.admin.orders.order.service.impl;

import com.sd20201.datn.core.admin.orders.order.model.request.OrderRequest;
import com.sd20201.datn.core.admin.orders.order.repository.OrderRepository;
import com.sd20201.datn.core.admin.orders.order.service.OrderService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.IMEI;
import com.sd20201.datn.entity.IMEISold;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.InvoiceDetail;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.repository.IMEIRepository;
import com.sd20201.datn.repository.IMEISoldRepository;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import com.sd20201.datn.repository.ProductDetailRepository;
import com.sd20201.datn.repository.StaffRepository;
import com.sd20201.datn.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final ProductDetailRepository productDetailRepository;
    private final IMEIRepository imeiRepository;
    private final IMEISoldRepository imeiSoldRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;

    @Override
    @Transactional
    public ResponseObject<Object> createOrderAtCounter(OrderRequest request) {
        // 1. Tạo hóa đơn (Invoice)
        Invoice invoice = new Invoice();

        // Gán mã hóa đơn tự động (hoặc bạn có thể dùng logic sinh mã riêng)
        invoice.setCode("HD" + Instant.now().getEpochSecond());

        // Gán nhân viên & khách hàng
        if (request.getStaffId() != null) {
            invoice.setStaff(staffRepository.findById(request.getStaffId()).orElse(null));
        }
        if (request.getCustomerId() != null) {
            invoice.setCustomer(customerRepository.findById(request.getCustomerId()).orElse(null));
        }
        // Gán voucher nếu có
        if (request.getVoucherId() != null) {
            invoice.setVoucher(voucherRepository.findById(request.getVoucherId()).orElse(null));
        }

        // Thông tin thanh toán
        invoice.setTotalAmount(request.getTotalAmount());
        invoice.setTotalAmountAfterDecrease(request.getTotalAmountAfterDecrease());
        invoice.setShippingFee(BigDecimal.ZERO); // Tại quầy thường không có ship
        invoice.setPaymentDate(Instant.now().toEpochMilli());

        // Loại hóa đơn: Tại quầy
        invoice.setTypeInvoice(TypeInvoice.OFFLINE); // Đảm bảo Enum này có giá trị tương ứng
        invoice.setDescription(request.getDescription());

        // Lưu thông tin người nhận (nếu khách lẻ cần lưu)
        invoice.setNameReceiver(request.getNameReceiver());
        invoice.setPhoneReceiver(request.getPhoneReceiver());

        // Save Invoice
        Invoice savedInvoice = orderRepository.save(invoice);

        List<InvoiceDetail> detailsToSave = new ArrayList<>();

        // 2. Xử lý từng sản phẩm trong giỏ hàng
        for (OrderRequest.ProductDetailOrder item : request.getProducts()) {
            ProductDetail productDetail = productDetailRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + item.getProductDetailId()));

            // Trường hợp 1: Sản phẩm có IMEI (Điện thoại, Laptop...)
            if (item.getListImeiIds() != null && !item.getListImeiIds().isEmpty()) {
                for (String imeiId : item.getListImeiIds()) {
                    // Tìm và validate IMEI
                    IMEI imei = imeiRepository.findById(imeiId)
                            .orElseThrow(() -> new RuntimeException("IMEI không tồn tại"));

                    if (imei.getStatus() != 1) { // Giả sử 1 là ACTIVE/IN_STOCK
                        throw new RuntimeException("IMEI " + imei.getImei() + " đã bán hoặc không khả dụng");
                    }

                    // Cập nhật trạng thái IMEI gốc -> Đã bán
                    imei.setStatus(0); // 0: SOLD
                    imeiRepository.save(imei);

                    // Tạo bản ghi IMEISold (Lịch sử bán)
                    IMEISold imeiSold = new IMEISold();
                    imeiSold.setImei(imei);
                    // imeiSold.setProduct(productDetail.getProduct()); // Nếu entity IMEISold có trường này
                    IMEISold savedImeiSold = imeiSoldRepository.save(imeiSold);

                    // Tạo InvoiceDetail cho từng IMEI (Số lượng luôn là 1)
                    InvoiceDetail detail = new InvoiceDetail();
                    detail.setInvoice(savedInvoice);
                    detail.setProductDetail(productDetail);
                    detail.setIMEISold(savedImeiSold); // Liên kết với bảng IMEISold
                    detail.setQuantity(1);
                    detail.setPrice(item.getPrice());
                    detail.setTotalAmount(item.getPrice()); // 1 * Price

                    detailsToSave.add(detail);
                }

                // Trừ tồn kho tổng của ProductDetail
                productDetail.setQuantity(productDetail.getQuantity() - item.getListImeiIds().size());
                productDetailRepository.save(productDetail);

            } else {
                // Trường hợp 2: Sản phẩm không có IMEI (Phụ kiện...)
                InvoiceDetail detail = new InvoiceDetail();
                detail.setInvoice(savedInvoice);
                detail.setProductDetail(productDetail);
                detail.setQuantity(item.getQuantity());
                detail.setPrice(item.getPrice());

                // Tính thành tiền: Price * Quantity
                BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                detail.setTotalAmount(lineTotal);

                detailsToSave.add(detail);

                // Trừ tồn kho
                productDetail.setQuantity(productDetail.getQuantity() - item.getQuantity());
                productDetailRepository.save(productDetail);
            }
        }

        // Lưu tất cả chi tiết hóa đơn
        invoiceDetailRepository.saveAll(detailsToSave);

        return new ResponseObject<>(savedInvoice, HttpStatus.CREATED, "Tạo đơn hàng thành công");
    }
}