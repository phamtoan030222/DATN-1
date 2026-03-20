package com.sd20201.datn.core.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sd20201.datn.core.payment.config.PayOSConfig;
import com.sd20201.datn.core.payment.dto.PayOSRequest;
import com.sd20201.datn.core.payment.dto.PayOSResponse;
import com.sd20201.datn.core.admin.banhang.repository.ADTaoHoaDonRepository;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.LichSuThanhToan;
import com.sd20201.datn.entity.LichSuTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.*;
import com.sd20201.datn.repository.InvoiceRepository;
import com.sd20201.datn.repository.LichSuThanhToanRepository;
import com.sd20201.datn.repository.LichSuTrangThaiHoaDonRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayOSService {

    private static final Logger logger = LoggerFactory.getLogger(PayOSService.class);
    private static final String PAYOS_CREATE_URL = "https://api-merchant.payos.vn/v2/payment-requests";

    private final PayOSConfig payOSConfig;
    private final InvoiceRepository invoiceRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final ADTaoHoaDonRepository adTaoHoaDonRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== TẠO LINK THANH TOÁN ====================
    public PayOSResponse createPaymentLink(PayOSRequest request) {
        PayOSResponse response = new PayOSResponse();
        try {
            Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                    .orElse(null);
            if (invoice == null) {
                response.setCode("01");
                response.setMessage("Không tìm thấy hóa đơn");
                return response;
            }

            if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                response.setCode("03");
                response.setMessage("Hóa đơn đã được thanh toán");
                return response;
            }

            BigDecimal amount = request.getAmount() != null
                    ? request.getAmount()
                    : invoice.getTotalAmount();

            long orderCode = System.currentTimeMillis() % 1_000_000_000L;

            String description = request.getDescription() != null
                    ? request.getDescription()
                    : "Thanh toan " + invoice.getCode();
            if (description.length() > 25) {
                description = description.substring(0, 25);
            }

            String returnUrl = (request.getReturnUrl() != null
                    && !request.getReturnUrl().isBlank())
                    ? request.getReturnUrl()
                    : payOSConfig.getReturnUrl() + "?invoiceId=" + invoice.getId();

            String cancelUrl = (request.getCancelUrl() != null
                    && !request.getCancelUrl().isBlank())
                    ? request.getCancelUrl()
                    : payOSConfig.getCancelUrl()
                    + "?invoiceId=" + invoice.getId()
                    + "&invoiceCode=" + invoice.getCode();

            // Build items
            Map<String, Object> item = new HashMap<>();
            item.put("name", "Thanh toan " + invoice.getCode());
            item.put("quantity", 1);
            item.put("price", amount.intValue());

            // Build body theo đúng thứ tự để tạo signature
            // PayOS yêu cầu signature = HMAC_SHA256 của chuỗi:
            // amount=xxx&cancelUrl=xxx&description=xxx&orderCode=xxx&returnUrl=xxx
            String signatureData = "amount=" + amount.intValue()
                    + "&cancelUrl=" + cancelUrl
                    + "&description=" + description
                    + "&orderCode=" + orderCode
                    + "&returnUrl=" + returnUrl;

            String signature = hmacSHA256(payOSConfig.getChecksumKey(), signatureData);

            // Build request body
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("orderCode",   orderCode);
            body.put("amount",      amount.intValue());
            body.put("description", description);
            body.put("items",       List.of(item));
            body.put("returnUrl",   returnUrl);
            body.put("cancelUrl",   cancelUrl);
            body.put("signature",   signature);

            String jsonBody = objectMapper.writeValueAsString(body);
            logger.info("PayOS request body: {}", jsonBody);

            // Gọi PayOS API
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(PAYOS_CREATE_URL))
                    .header("Content-Type",  "application/json")
                    .header("x-client-id",   payOSConfig.getClientId())
                    .header("x-api-key",     payOSConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> httpResponse = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            logger.info("PayOS response: {}", httpResponse.body());

            Map<String, Object> result = objectMapper.readValue(
                    httpResponse.body(), Map.class);

            String code    = (String) result.get("code");
            String message = (String) result.get("desc");

            if ("00".equals(code)) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");

                String checkoutUrl    = (String) data.get("checkoutUrl");
                String qrCode         = (String) data.get("qrCode");
                String paymentLinkId  = (String) data.get("paymentLinkId");

                // Cập nhật trạng thái hóa đơn
                invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHO_THANH_TOAN);
                invoiceRepository.save(invoice);

                // Lưu lịch sử
                LichSuThanhToan lichSu = new LichSuThanhToan();
                lichSu.setHoaDon(invoice);
                lichSu.setSoTien(amount);
                lichSu.setLoaiGiaoDich("PAYOS_CREATE");
                lichSu.setMaGiaoDich(String.valueOf(orderCode));
                lichSu.setThoiGian(LocalDateTime.now());
                lichSu.setTrangThaiThanhToan(TrangThaiThanhToan.CHO_THANH_TOAN);
                lichSu.setGhiChu("Tạo PayOS - orderCode: " + orderCode
                        + " | paymentLinkId: " + paymentLinkId);
                lichSuThanhToanRepository.save(lichSu);

                logger.info("Tạo PayOS thành công: invoice={}, orderCode={}",
                        invoice.getCode(), orderCode);

                response.setCode("00");
                response.setMessage("Success");
                response.setCheckoutUrl(checkoutUrl);
                response.setQrCode(qrCode);
                response.setPaymentLinkId(paymentLinkId);
                response.setOrderCode(String.valueOf(orderCode));
                response.setAmount(amount);
                response.setInvoiceId(invoice.getId());
                response.setInvoiceCode(invoice.getCode());

            } else {
                response.setCode("99");
                response.setMessage(message);
            }

        } catch (Exception e) {
            logger.error("Lỗi tạo PayOS: ", e);
            response.setCode("99");
            response.setMessage("Lỗi hệ thống: " + e.getMessage());
        }
        return response;
    }

    // ==================== XỬ LÝ RETURN ====================
    @Transactional
    public Map<String, Object> processReturn(String orderCode, String status,
                                             String invoiceId) {
        try {
            logger.info("PayOS return: orderCode={}, status={}, invoiceId={}",
                    orderCode, status, invoiceId);

            Invoice invoice = null;

            if (invoiceId != null && !invoiceId.isBlank()) {
                invoice = invoiceRepository.findById(invoiceId).orElse(null);
            }
            if (invoice == null && orderCode != null) {
                invoice = findInvoiceByOrderCode(orderCode);
            }
            if (invoice == null) {
                return Map.of("code", "FAILED", "message", "Không tìm thấy hóa đơn");
            }

            if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                return Map.of("code", "00", "orderCode", invoice.getCode());
            }

            if ("PAID".equals(status)) {
                BigDecimal amount = invoice.getTotalAmount() != null
                        ? invoice.getTotalAmount()
                        : invoice.getTotalAmountAfterDecrease();

                xuLyThanhToanThanhCongPayOS(
                        invoice,
                        orderCode != null ? orderCode : "PAYOS_" + System.currentTimeMillis(),
                        amount
                );
                return Map.of("code", "00", "orderCode", invoice.getCode());

            } else {
                invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
                invoiceRepository.save(invoice);
                Map<String, Object> result = new HashMap<>();
                result.put("code",        "CANCELLED");
                result.put("message",     "Thanh toán bị hủy");
                result.put("invoiceId",   invoice.getId());
                result.put("invoiceCode", invoice.getCode());
                return result;
            }

        } catch (Exception e) {
            logger.error("Lỗi PayOS processReturn: ", e);
            return Map.of("code", "FAILED", "message", e.getMessage());
        }
    }

    // ==================== WEBHOOK ====================
    @Transactional
    public Map<String, Object> processWebhook(Map<String, Object> webhookBody) {
        try {
            logger.info("=== PAYOS WEBHOOK: {}", webhookBody);

            // Verify signature
            if (!verifyWebhookSignature(webhookBody)) {
                logger.warn("PayOS webhook - Chữ ký không hợp lệ");
                return Map.of("error", -1, "message", "Invalid signature");
            }

            Map<String, Object> data = (Map<String, Object>) webhookBody.get("data");
            if (data == null) return Map.of("error", 0);

            String orderCode = String.valueOf(data.get("orderCode"));
            String code      = (String) webhookBody.get("code");
            int    amount    = ((Number) data.get("amount")).intValue();

            Invoice invoice = findInvoiceByOrderCode(orderCode);
            if (invoice == null) {
                logger.warn("Không tìm thấy invoice: orderCode={}", orderCode);
                return Map.of("error", 0);
            }

            if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                return Map.of("error", 0);
            }

            if ("00".equals(code)) {
                xuLyThanhToanThanhCongPayOS(invoice, orderCode, new BigDecimal(amount));
            } else {
                invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
                invoiceRepository.save(invoice);
            }

            return Map.of("error", 0);

        } catch (Exception e) {
            logger.error("Lỗi PayOS webhook: ", e);
            return Map.of("error", -1, "message", e.getMessage());
        }
    }

    // ==================== POLLING ====================
    public Map<String, Object> checkStatus(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .map(invoice -> {
                    boolean isPaid = TrangThaiThanhToan.DA_THANH_TOAN
                            .equals(invoice.getTrangThaiThanhToan());
                    logger.info("=== CHECK STATUS: invoiceId={}, isPaid={}, code={}, typePayment={}",
                            invoiceId, isPaid, invoice.getCode(), invoice.getTypePayment());
                    String status = invoice.getTrangThaiThanhToan() != null
                            ? invoice.getTrangThaiThanhToan().name()
                            : "CHUA_THANH_TOAN";
                    Map<String, Object> result = new HashMap<>();
                    result.put("status",      status);
                    result.put("isPaid",      isPaid);
                    result.put("invoiceCode", invoice.getCode() != null
                            ? invoice.getCode() : "");
                    return result;
                })
                .orElseGet(() -> {
                    Map<String, Object> notFound = new HashMap<>();
                    notFound.put("status", "NOT_FOUND");
                    notFound.put("isPaid", false);
                    return notFound;
                });
    }

    // ==================== CORE PROCESSING ====================
    @Transactional
    public void xuLyThanhToanThanhCongPayOS(Invoice invoice, String orderCode,
                                            BigDecimal amount) {
        EntityTrangThaiHoaDon trangThai;
        String statusNote;

        if (invoice.getTypeInvoice() == TypeInvoice.ONLINE) {
            trangThai  = EntityTrangThaiHoaDon.CHO_XAC_NHAN;
            statusNote = "Đơn online thanh toán PayOS - chờ shop xác nhận";
        } else if (invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG
                || invoice.getTypeInvoice() == TypeInvoice.ONLINE_TAI_QUAY) {
            trangThai  = EntityTrangThaiHoaDon.DA_XAC_NHAN;
            statusNote = "Hóa đơn giao hàng thanh toán PayOS thành công";
        } else {
            trangThai  = EntityTrangThaiHoaDon.HOAN_THANH;
            statusNote = "Hóa đơn tại quầy thanh toán PayOS thành công";
        }

        invoice.setEntityTrangThaiHoaDon(trangThai);
        invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        invoice.setTypePayment(TypePayment.VIETQR);
        invoice.setTransactionId(orderCode);
        invoice.setPaymentDate(System.currentTimeMillis());
        Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);

        LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
        history.setHoaDon(savedInvoice);
        history.setTrangThai(trangThai);
        history.setNote(statusNote);
        history.setThoiGian(LocalDateTime.now());
        lichSuTrangThaiHoaDonRepository.save(history);

        LichSuThanhToan lichSu = new LichSuThanhToan();
        lichSu.setHoaDon(savedInvoice);
        lichSu.setSoTien(amount);
        lichSu.setLoaiGiaoDich("PAYOS");
        lichSu.setMaGiaoDich(orderCode);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        lichSu.setGhiChu("Thanh toán PayOS - orderCode: " + orderCode);
        lichSuThanhToanRepository.save(lichSu);

        logger.info("PayOS xong: invoice={}, trạng thái={}", invoice.getCode(), trangThai);
    }

    // ==================== PRIVATE HELPERS ====================
    private Invoice findInvoiceByOrderCode(String orderCode) {
        try {
            return lichSuThanhToanRepository
                    .findFirstByMaGiaoDichAndLoaiGiaoDich(orderCode, "PAYOS_CREATE")
                    .map(LichSuThanhToan::getHoaDon)
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Lỗi tìm invoice theo orderCode: ", e);
            return null;
        }
    }

    private boolean verifyWebhookSignature(Map<String, Object> payload) {
        try {
            String signature = (String) payload.get("signature");
            if (signature == null) return false;

            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data == null) return false;

            // PayOS signature = HMAC_SHA256(amount + cancelUrl + description + orderCode + returnUrl)
            String rawData = "amount=" + data.get("amount")
                    + "&cancelUrl=" + data.getOrDefault("cancelUrl", "")
                    + "&description=" + data.getOrDefault("description", "")
                    + "&orderCode=" + data.get("orderCode")
                    + "&returnUrl=" + data.getOrDefault("returnUrl", "");

            String computed = hmacSHA256(payOSConfig.getChecksumKey(), rawData);
            return computed.equals(signature);
        } catch (Exception e) {
            logger.error("Lỗi verify PayOS webhook: ", e);
            return false;
        }
    }

    private String hmacSHA256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}