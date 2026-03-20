package com.sd20201.datn.core.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sd20201.datn.core.payment.config.MomoConfig;
import com.sd20201.datn.core.payment.dto.MomoPaymentResponse;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.LichSuThanhToan;
import com.sd20201.datn.entity.LichSuTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.*;
import com.sd20201.datn.repository.InvoiceRepository;
import com.sd20201.datn.repository.LichSuThanhToanRepository;
import com.sd20201.datn.repository.LichSuTrangThaiHoaDonRepository;
import com.sd20201.datn.core.admin.banhang.repository.ADTaoHoaDonRepository;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MomoService {

    private static final Logger logger = LoggerFactory.getLogger(MomoService.class);

    private final MomoConfig momoConfig;
    private final InvoiceRepository invoiceRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final ADTaoHoaDonRepository adTaoHoaDonRepository;

    // ==================== TẠO THANH TOÁN ====================
    public Map<String, Object> createPayment(String invoiceId, BigDecimal amount,
                                             String orderInfo, String returnUrl) {
        try {
            String requestId = UUID.randomUUID().toString();
            String orderId   = invoiceId + "_" + System.currentTimeMillis();
            String amountStr = String.valueOf(amount.longValue());

            orderInfo = "MYLAPTOP_CART_" + invoiceId;

            // Dùng returnUrl từ tham số nếu có, fallback về config
            String finalReturnUrl = (returnUrl != null && !returnUrl.isBlank())
                    ? returnUrl : momoConfig.getReturnUrl();

            // Build raw signature (MoMo sẽ tạo mã QR với đúng orderInfo này)
            String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                    + "&amount=" + amountStr
                    + "&extraData="
                    + "&ipnUrl=" + momoConfig.getNotifyUrl()
                    + "&orderId=" + orderId
                    + "&orderInfo=" + orderInfo
                    + "&partnerCode=" + momoConfig.getPartnerCode()
                    + "&redirectUrl=" + finalReturnUrl
                    + "&requestId=" + requestId
                    + "&requestType=" + momoConfig.getRequestType();

            String signature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);

            // Build request body
            Map<String, Object> body = new HashMap<>();
            body.put("partnerCode",  momoConfig.getPartnerCode());
            body.put("accessKey",    momoConfig.getAccessKey());
            body.put("requestId",    requestId);
            body.put("amount",       amountStr);
            body.put("orderId",      orderId);
            body.put("orderInfo",    orderInfo); // Gửi mô tả mới lên MoMo
            body.put("redirectUrl",  finalReturnUrl);
            body.put("ipnUrl",       momoConfig.getNotifyUrl());
            body.put("requestType",  momoConfig.getRequestType());
            body.put("extraData",    "");
            body.put("lang",         "vi");
            body.put("signature",    signature);

            // Gọi MoMo API
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(body);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(momoConfig.getEndpoint()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            MomoPaymentResponse momoRes = mapper.readValue(
                    response.body(), MomoPaymentResponse.class);

            logger.info("MoMo response: resultCode={}, message={}",
                    momoRes.getResultCode(), momoRes.getMessage());

            if (momoRes.getResultCode() == 0) {
                // Lưu trạng thái chờ thanh toán
                invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                    invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHO_THANH_TOAN);
                    invoiceRepository.save(invoice);
                });

                Map<String, Object> result = new HashMap<>();
                result.put("code",    "00");
                result.put("payUrl",  momoRes.getPayUrl());
                result.put("orderId", orderId);
                result.put("message", "Success");
                return result;
            } else {
                return Map.of(
                        "code", "99",
                        "message", momoRes.getMessage()
                );
            }

        } catch (Exception e) {
            logger.error("Lỗi tạo thanh toán MoMo: ", e);
            return Map.of("code", "99", "message", e.getMessage());
        }
    }

    // ==================== XỬ LÝ RETURN ====================
    @Transactional
    public Map<String, Object> processReturn(Map<String, String> params) {
        try {
            logger.info("=== MOMO RETURN: {}", params);

            String resultCode = params.get("resultCode");
            String orderId    = params.get("orderId");
            String invoiceId  = orderId != null ? orderId.split("_")[0] : "";
            String transId    = params.get("transId");
            String amountStr  = params.get("amount");

            if (!verifyReturnSignature(params)) {
                logger.warn("MoMo return - Chữ ký không hợp lệ");
                return Map.of("code", "97", "message", "Chữ ký không hợp lệ");
            }

            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                return Map.of("code", "01", "message", "Không tìm thấy hóa đơn");
            }

            if ("0".equals(resultCode)) {
                // Chống trùng lặp
                if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                    return Map.of("code", "00", "message", "Đã xử lý",
                            "orderCode", invoice.getCode());
                }

                BigDecimal amount = amountStr != null
                        ? new BigDecimal(amountStr) : BigDecimal.ZERO;

                // Cập nhật LUU_TAM → CHO_XAC_NHAN
                invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
                invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
                invoice.setTypePayment(TypePayment.MOMO);
                invoice.setTransactionId(transId);
                invoice.setPaymentDate(System.currentTimeMillis());
                Invoice saved = adTaoHoaDonRepository.save(invoice);

                LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
                history.setHoaDon(saved);
                history.setTrangThai(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
                history.setNote("Thanh toán MoMo thành công - chờ xác nhận");
                history.setThoiGian(LocalDateTime.now());
                lichSuTrangThaiHoaDonRepository.save(history);

                LichSuThanhToan lichSu = new LichSuThanhToan();
                lichSu.setHoaDon(saved);
                lichSu.setSoTien(amount);
                lichSu.setLoaiGiaoDich("MOMO");
                lichSu.setMaGiaoDich(transId);
                lichSu.setThoiGian(LocalDateTime.now());
                lichSu.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
                lichSu.setGhiChu("MoMo return - TransId: " + transId);
                lichSuThanhToanRepository.save(lichSu);

                return Map.of("code", "00", "message", "Thành công",
                        "orderCode", saved.getCode());
            } else {
                // Hủy / thất bại → giữ nguyên LUU_TAM
                logger.info("MoMo return thất bại - invoiceId: {}, resultCode: {}",
                        invoiceId, resultCode);
                return Map.of("code", resultCode,
                        "message", "Thanh toán thất bại hoặc bị hủy",
                        "invoiceId", invoiceId);
            }

        } catch (Exception e) {
            logger.error("Lỗi processReturn MoMo: ", e);
            return Map.of("code", "99", "message", e.getMessage());
        }
    }

    // ==================== XỬ LÝ THANH TOÁN THÀNH CÔNG ====================
    @Transactional
    public void xuLyThanhToanThanhCongMomo(Invoice invoice,
                                           String transId, BigDecimal amount) {
        if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
            logger.info("Hóa đơn đã được thanh toán trước đó");
            return;
        }

        // Set trạng thái theo loại hóa đơn
        EntityTrangThaiHoaDon trangThai;
        String note;

        if (invoice.getTypeInvoice() == TypeInvoice.ONLINE) {
            trangThai = EntityTrangThaiHoaDon.CHO_XAC_NHAN; // ← Online: chờ xác nhận
            note = "Đơn hàng online thanh toán MoMo thành công - chờ shop xác nhận";
        } else if (invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG || invoice.getTypeInvoice() == TypeInvoice.ONLINE_TAI_QUAY) {
            trangThai = EntityTrangThaiHoaDon.DA_XAC_NHAN;
            note = "Hóa đơn giao hàng thanh toán MoMo thành công";
        } else {
            trangThai = EntityTrangThaiHoaDon.HOAN_THANH; // ← Tại quầy: hoàn thành
            note = "Hóa đơn tại quầy thanh toán MoMo thành công";
        }

        invoice.setEntityTrangThaiHoaDon(trangThai);
        invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        invoice.setTypePayment(TypePayment.MOMO);
        invoice.setTransactionId(transId);
        invoice.setPaymentDate(System.currentTimeMillis());

        Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);

        // Lưu lịch sử trạng thái
        LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
        history.setHoaDon(savedInvoice);
        history.setTrangThai(trangThai);
        history.setNote(note);
        history.setThoiGian(LocalDateTime.now());
        lichSuTrangThaiHoaDonRepository.save(history);

        // Lưu lịch sử thanh toán
        LichSuThanhToan lichSu = new LichSuThanhToan();
        lichSu.setHoaDon(savedInvoice);
        lichSu.setSoTien(amount);
        lichSu.setLoaiGiaoDich("MOMO");
        lichSu.setMaGiaoDich(transId);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        lichSu.setGhiChu("Thanh toán MoMo thành công - TransId: " + transId);
        lichSuThanhToanRepository.save(lichSu);

        logger.info("Đã xử lý MoMo thành công, trạng thái: {}", trangThai);
    }

    // ==================== HELPERS ====================
    private String hmacSHA256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private boolean verifyReturnSignature(Map<String, String> params) {
        try {
            String signature = params.get("signature");
            if (signature == null) return false;

            String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                    + "&amount=" + params.getOrDefault("amount", "")
                    + "&extraData=" + params.getOrDefault("extraData", "")
                    + "&message=" + params.getOrDefault("message", "")
                    + "&orderId=" + params.getOrDefault("orderId", "")
                    + "&orderInfo=" + params.getOrDefault("orderInfo", "")
                    + "&orderType=" + params.getOrDefault("orderType", "")
                    + "&partnerCode=" + params.getOrDefault("partnerCode", "")
                    + "&payType=" + params.getOrDefault("payType", "")
                    + "&requestId=" + params.getOrDefault("requestId", "")
                    + "&responseTime=" + params.getOrDefault("responseTime", "")
                    + "&resultCode=" + params.getOrDefault("resultCode", "")
                    + "&transId=" + params.getOrDefault("transId", "");

            String computed = hmacSHA256(momoConfig.getSecretKey(), rawSignature);
            return computed.equals(signature);
        } catch (Exception e) {
            logger.error("Lỗi verify MoMo signature: ", e);
            return false;
        }
    }
}