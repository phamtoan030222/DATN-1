package com.sd20201.datn.core.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sd20201.datn.core.payment.config.ZaloPayConfig;
import com.sd20201.datn.core.admin.banhang.repository.ADTaoHoaDonRepository;
import com.sd20201.datn.core.payment.dto.ZaloPayResponse;
import com.sd20201.datn.entity.*;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ZaloPayService {

    private static final Logger logger = LoggerFactory.getLogger(ZaloPayService.class);

    private final ZaloPayConfig zaloPayConfig;
    private final InvoiceRepository invoiceRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final ADTaoHoaDonRepository adTaoHoaDonRepository;

    // ==================== TẠO THANH TOÁN ====================
    public Map<String, Object> createPayment(String invoiceId, BigDecimal amount,
                                             String description) {
        try {
            String appId      = zaloPayConfig.getAppId();
            String appUser    = "user_" + invoiceId;
            String appTime    = String.valueOf(System.currentTimeMillis());
            String shortInvoiceId = invoiceId.replace("-", "").substring(0, 8);
            String shortTime      = String.valueOf(System.currentTimeMillis()).substring(5); // 8 số
            String appTransId     = new SimpleDateFormat("yyMMdd").format(new Date())
                    + "_" + shortInvoiceId + "_" + shortTime;
            String amountStr  = String.valueOf(amount.longValue());

            Map<String, String> embedData = new HashMap<>();
            embedData.put("redirecturl", zaloPayConfig.getRedirectUrl()
                    + "?invoiceId=" + invoiceId);
            embedData.put("invoiceId", invoiceId); // ← THÊM để dùng trong callback
            ObjectMapper mapper = new ObjectMapper();
            String embedDataStr = mapper.writeValueAsString(embedData);

            String itemStr = "[]";

            // Build MAC
            String rawMac = appId + "|" + appTransId + "|" + appUser + "|"
                    + amountStr + "|" + appTime + "|" + embedDataStr + "|" + itemStr;
            String mac = hmacSHA256(zaloPayConfig.getKey1(), rawMac);

            // Build request body
            Map<String, Object> body = new HashMap<>();
            body.put("app_id",       Integer.parseInt(appId));
            body.put("app_user",     appUser);
            body.put("app_time",     Long.parseLong(appTime));
            body.put("amount",       Long.parseLong(amountStr));
            body.put("app_trans_id", appTransId);
            body.put("embed_data",   embedDataStr);
            body.put("item",         itemStr);
            body.put("description",  description);
            body.put("bank_code",    "");
            body.put("callback_url", zaloPayConfig.getCallbackUrl());
            body.put("mac",          mac);

            // Gọi ZaloPay API
            String jsonBody = mapper.writeValueAsString(body);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(zaloPayConfig.getEndpoint()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            logger.info("=== ZALOPAY RAW RESPONSE: {}", response.body());

            // ✅ Dùng ZaloPayResponse DTO thay vì Map
            ObjectMapper snakeMapper = new ObjectMapper();
            snakeMapper.setPropertyNamingStrategy(
                    com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
            ZaloPayResponse zaloRes = snakeMapper.readValue(
                    response.body(), ZaloPayResponse.class);

            logger.info("ZaloPay response: returnCode={}, message={}",
                    zaloRes.getReturnCode(), zaloRes.getReturnMessage());

            if (zaloRes.getReturnCode() == 1) {
                invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                    invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHO_THANH_TOAN_VNPAY);
                    invoiceRepository.save(invoice);
                });

                Map<String, Object> success = new HashMap<>();
                success.put("code",    "00");
                success.put("payUrl",  zaloRes.getOrderUrl()); // ← Dùng DTO
                success.put("transId", appTransId);
                success.put("message", "Success");
                return success;
            }
            else {
                Map<String, Object> fail = new HashMap<>();
                fail.put("code",    "99");
                fail.put("message", zaloRes.getReturnMessage());
                return fail;
            }

        } catch (Exception e) {
            logger.error("Lỗi tạo thanh toán ZaloPay: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("code",    "99");
            error.put("message", e.getMessage());
            return error;
        }
    }

    // ==================== XỬ LÝ CALLBACK (IPN) ====================
    @Transactional
    public Map<String, Object> processCallback(Map<String, Object> params) {
        try {
            logger.info("=== ZALOPAY CALLBACK: {}", params);

            // Verify MAC
            String data     = (String) params.get("data");
            String reqMac   = (String) params.get("mac");
            String computedMac = hmacSHA256(zaloPayConfig.getKey2(), data);

            if (!computedMac.equals(reqMac)) {
                logger.warn("ZaloPay callback - MAC không hợp lệ");
                Map<String, Object> fail = new HashMap<>();
                fail.put("return_code",    -1);
                fail.put("return_message", "mac not equal");
                return fail;
            }

            // Parse data
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> dataMap = mapper.readValue(data, Map.class);

            String appTransId   = (String) dataMap.get("app_trans_id");
            String embedDataStr = (String) dataMap.get("embed_data");

            String invoiceId;
            try {
                Map<String, String> embedMap = mapper.readValue(embedDataStr, Map.class);
                invoiceId = embedMap.get("invoiceId"); // ← Lấy invoiceId đầy đủ từ embed_data
            } catch (Exception e) {
                logger.error("Không parse được embed_data: {}", e.getMessage());
                Map<String, Object> fail = new HashMap<>();
                fail.put("return_code",    -1);
                fail.put("return_message", "cannot parse invoiceId");
                return fail;
            }
            long   amount     = ((Number) dataMap.get("amount")).longValue();

            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("return_code",    -1);
                fail.put("return_message", "invoice not found");
                return fail;
            }

            xuLyThanhToanThanhCongZaloPay(invoice, appTransId,
                    new BigDecimal(amount));

            Map<String, Object> success = new HashMap<>();
            success.put("return_code",    1);
            success.put("return_message", "success");
            return success;

        } catch (Exception e) {
            logger.error("Lỗi xử lý ZaloPay callback: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("return_code",    -1);
            error.put("return_message", e.getMessage());
            return error;
        }
    }

    // ==================== XỬ LÝ RETURN ====================
    public Map<String, Object> processReturn(String invoiceId, String status) {
        // Nếu không có invoiceId → user hủy
        if (invoiceId == null || invoiceId.isBlank()) {
            Map<String, Object> fail = new HashMap<>();
            fail.put("code",    "FAILED");
            fail.put("message", "Thanh toán bị hủy");
            return fail;
        }

        try {
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("code",    "FAILED");
                fail.put("message", "Không tìm thấy hóa đơn");
                return fail;
            }

            if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                Map<String, Object> success = new HashMap<>();
                success.put("code",      "00");
                success.put("orderCode", invoice.getCode());
                return success;
            }

            // Chưa thanh toán = user hủy
            Map<String, Object> fail = new HashMap<>();
            fail.put("code",    "FAILED");
            fail.put("message", "Thanh toán bị hủy hoặc chưa hoàn thành");
            return fail;

        } catch (Exception e) {
            Map<String, Object> fail = new HashMap<>();
            fail.put("code",    "FAILED");
            fail.put("message", e.getMessage());
            return fail;
        }
    }

    // ==================== XỬ LÝ THANH TOÁN THÀNH CÔNG ====================
    @Transactional
    public void xuLyThanhToanThanhCongZaloPay(Invoice invoice,
                                              String transId, BigDecimal amount) {
        if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
            logger.info("Hóa đơn đã được thanh toán trước đó");
            return;
        }

        EntityTrangThaiHoaDon trangThai;
        String note;

        if (invoice.getTypeInvoice() == TypeInvoice.ONLINE) {
            trangThai = EntityTrangThaiHoaDon.CHO_XAC_NHAN;
            note = "Đơn hàng online thanh toán ZaloPay - chờ shop xác nhận";
        } else if (invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
            trangThai = EntityTrangThaiHoaDon.DA_XAC_NHAN;
            note = "Hóa đơn giao hàng thanh toán ZaloPay thành công";
        } else {
            trangThai = EntityTrangThaiHoaDon.HOAN_THANH;
            note = "Hóa đơn tại quầy thanh toán ZaloPay thành công";
        }

        invoice.setEntityTrangThaiHoaDon(trangThai);
        invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        invoice.setTypePayment(TypePayment.ZALOPAY);
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
        lichSu.setLoaiGiaoDich("ZALOPAY");
        lichSu.setMaGiaoDich(transId);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        lichSu.setGhiChu("Thanh toán ZaloPay - TransId: " + transId);
        lichSuThanhToanRepository.save(lichSu);

        logger.info("Đã xử lý ZaloPay thành công, trạng thái: {}", trangThai);
    }

    // ==================== HELPERS ====================
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