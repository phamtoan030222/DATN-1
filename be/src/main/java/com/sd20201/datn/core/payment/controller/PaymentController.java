package com.sd20201.datn.core.payment.controller;

import com.sd20201.datn.core.payment.dto.IpnResponse;
import com.sd20201.datn.core.payment.dto.PaymentRequest;
import com.sd20201.datn.core.payment.dto.PaymentResponse;
import com.sd20201.datn.core.payment.service.VnPayService;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.repository.InvoiceRepository; // Nhớ import cái này
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor // Dùng annotation này thì các biến 'final' sẽ tự động được inject
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    // Bỏ @Autowired đi và thêm chữ 'final' cho đúng chuẩn Lombok @RequiredArgsConstructor
    private final VnPayService vnPayService;

    // BỔ SUNG: Khai báo InvoiceRepository để dùng cho hàm check-status
    private final InvoiceRepository invoiceRepository;

    @PostMapping("/create-vnpay")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        logger.info("Nhận request tạo thanh toán cho invoice: {}", request.getOrderId());

        PaymentResponse response = vnPayService.createPayment(request, httpRequest);

        if ("00".equals(response.getCode())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<PaymentResponse> paymentReturn(@RequestParam Map<String, String> params) {
        logger.info("Nhận return từ VNPAY với params: {}", params.keySet());

        PaymentResponse response = vnPayService.processReturn(params);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<IpnResponse> paymentIpn(@RequestParam Map<String, String> params) {
        logger.info("Nhận IPN từ VNPAY");

        IpnResponse response = vnPayService.processIpn(params);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-return-page")
    public String paymentReturnPage(@RequestParam Map<String, String> params) {
        PaymentResponse response = vnPayService.processReturn(params);

        if ("00".equals(response.getCode())) {
            return "<html>" +
                    "<head><title>Thanh toán thành công</title><meta charset='UTF-8'>" +
                    "<style>" +
                    "body{font-family:Arial,sans-serif;text-align:center;padding:50px;}" +
                    ".success{color:#28a745;}.info{margin:20px 0;}" +
                    ".btn{display:inline-block;padding:10px 20px;background:#007bff;color:white;text-decoration:none;border-radius:5px;}" +
                    "</style></head>" +
                    "<body>" +
                    "<h1 class='success'>✅ Thanh toán thành công!</h1>" +
                    "<div class='info'>" +
                    "<p>Mã hóa đơn: <strong>" + response.getOrderId() + "</strong></p>" +
                    "<p>Mã giao dịch: <strong>" + response.getTransactionId() + "</strong></p>" +
                    "<p>Số tiền: <strong>" + String.format("%,d", response.getAmount().longValue()) + " VND</strong></p>" +
                    "</div>" +
                    "<p><a href='#' class='btn' onclick='window.close()'>Đóng trang</a></p>" +
                    "<script>" +
                    // ← THÊM ĐOẠN NÀY: Gửi message về tab cha
                    "try {" +
                    "  if (window.opener && !window.opener.closed) {" +
                    "    window.opener.postMessage({" +
                    "      type: 'VNPAY_SUCCESS'," +
                    "      orderId: '" + response.getOrderId() + "'," +
                    "      transactionId: '" + response.getTransactionId() + "'," +
                    "      amount: " + response.getAmount().longValue() +
                    "    }, '*');" +
                    "  }" +
                    "} catch(e) { console.error('postMessage error', e); }" +
                    "setTimeout(() => window.close(), 3000);" +  // ← Tự đóng sau 3s
                    "<script>" +
                    "</body></html>";
        } else {
            return "<html>" +
                    "<head><title>Thanh toán thất bại</title><meta charset='UTF-8'>" +
                    "<style>" +
                    "body{font-family:Arial,sans-serif;text-align:center;padding:50px;}" +
                    ".error{color:#dc3545;}.info{margin:20px 0;}" +
                    ".btn{display:inline-block;padding:10px 20px;background:#007bff;color:white;text-decoration:none;border-radius:5px;}" +
                    "</style></head>" +
                    "<body>" +
                    "<h1 class='error'>❌ Thanh toán thất bại!</h1>" +
                    "<div class='info'>" +
                    "<p>Mã lỗi: <strong>" + response.getCode() + "</strong></p>" +
                    "<p>Chi tiết: <strong>" + response.getMessage() + "</strong></p>" +
                    "</div>" +
                    "<p><a href='#' class='btn' onclick='window.close()'>Đóng trang</a></p>" +
                    "<script>setTimeout(() => window.close(), 5000);<script>" +
                    "</body></html>";
        }
    }

    @GetMapping("/check-status/{idHD}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String idHD) {
        // Tìm hóa đơn trong DB
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(idHD);

        if (invoiceOpt.isPresent()) {
            Invoice invoice = invoiceOpt.get();
            Map<String, Object> response = new HashMap<>();

            // Lấy trạng thái thanh toán hiện tại trả về cho Frontend
            // Chú ý: Tránh lỗi NullPointerException nếu getTrangThaiThanhToan() vô tình bị null
            if (invoice.getTrangThaiThanhToan() != null) {
                response.put("trangThai", invoice.getTrangThaiThanhToan().name());
            } else {
                response.put("trangThai", "CHUA_THANH_TOAN");
            }

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body("Không tìm thấy hóa đơn");
    }
}