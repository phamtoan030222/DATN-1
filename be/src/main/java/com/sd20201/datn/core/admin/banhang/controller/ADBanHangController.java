package com.sd20201.datn.core.admin.banhang.controller;

import com.sd20201.datn.core.admin.banhang.model.request.*;
import com.sd20201.datn.core.admin.banhang.model.response.ADChonKhachHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADGioHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADPhuongThucThanhToanRespones;
import com.sd20201.datn.core.admin.banhang.model.response.ListHoaDon;
import com.sd20201.datn.core.admin.banhang.model.response.VoucherSuggestionResponse;
import com.sd20201.datn.core.admin.banhang.service.ADBanHangService;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.service.ADProductDetailService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sd20201.datn.infrastructure.geo.ShippingFeeRequest;
import com.sd20201.datn.infrastructure.geo.ShippingFeeResponse;
import com.sd20201.datn.infrastructure.geo.ShippingFeeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_ADMIN_BAN_HANG)
@Slf4j
@CrossOrigin(origins = "*")
public class ADBanHangController {
    private final ADProductDetailService productDetailService;
    public final ADBanHangService adBanHangService;

    private final java.util.concurrent.CopyOnWriteArrayList<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> emitters = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final ShippingFeeService shippingFeeService;

    @GetMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamPOS() {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(600000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        return emitter;
    }

    private void sendRealtimeUpdate(String message) {
        for (org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter : emitters) {
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("message").data(message));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    // =====================================
    // CÁC API SSE & THANH TOÁN QR
    // =====================================

    @PostMapping("/yeu-cau-qr/{id}")
    public ResponseEntity<?> yeuCauQR(@PathVariable("id") String id, @RequestParam(value = "qrUrl", required = false) String qrUrl) {
        String safeUrl = (qrUrl != null) ? qrUrl : "";
        String jsonMsg = "{\"action\": \"SHOW_QR\", \"idHD\": \"" + id + "\", \"qrUrl\": \"" + safeUrl + "\"}";
        sendRealtimeUpdate(jsonMsg);
        return ResponseEntity.ok("Đã gửi yêu cầu hiển thị QR VNPAY");
    }

    @PostMapping("/huy-yeu-cau-qr/{id}")
    public ResponseEntity<?> huyYeuCauQR(@PathVariable("id") String id) {
        String jsonMsg = "{\"action\": \"CANCEL_QR\", \"idHD\": \"" + id + "\"}";
        sendRealtimeUpdate(jsonMsg);
        return ResponseEntity.ok("Đã hủy QR");
    }

    @PostMapping("/payment-success-notify/{id}")
    public ResponseEntity<?> notifyPaymentSuccess(@PathVariable("id") String id) {
        String jsonMsg = "{\"action\": \"PAYMENT_SUCCESS\", \"idHD\": \"" + id + "\"}";
        sendRealtimeUpdate(jsonMsg);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/thanh-toan-thanh-cong")
    public ResponseEntity<?> thanhToanThanhCong(@RequestBody ADThanhToanRequest id) throws BadRequestException {
        ResponseEntity<?> response = Helper.createResponseEntity(adBanHangService.thanhToanThanhCong(id));
        String jsonMsg = "{\"action\": \"PAYMENT_SUCCESS\", \"idHD\": \"" + id.getIdHD() + "\"}";
        sendRealtimeUpdate(jsonMsg);
        return response;
    }

    // =====================================
    // CÁC API HOÁ ĐƠN & GIỎ HÀNG
    // =====================================

    @GetMapping("/list-hoa-don")
    public List<ListHoaDon> getListHoaDon() {
        return adBanHangService.getHoaDon();
    }

    @PostMapping("/create-hoa-don")
    public ResponseEntity<?> createHoaDon(@RequestBody ADNhanVienRequest adNhanVienRequest) {
        ResponseEntity<?> response = Helper.createResponseEntity(adBanHangService.createHoaDon(adNhanVienRequest));
        sendRealtimeUpdate("{\"action\": \"UPDATE_CART\"}");
        return response;
    }

    @GetMapping("/list-gio-hang/{id}")
    public List<ADGioHangResponse> getListGioHang(@PathVariable("id") String id) {
        return adBanHangService.getListGioHang(id);
    }

    @PostMapping("/huy")
    public ResponseEntity<?> huyHoaDon(ADHuyRequest adNhanVienRequest) {
        ResponseEntity<?> response = Helper.createResponseEntity(adBanHangService.huy(adNhanVienRequest));
        sendRealtimeUpdate("{\"action\": \"UPDATE_CART\"}");
        return response;
    }

    // =====================================
    // CÁC API SẢN PHẨM
    // =====================================

    @GetMapping("/san-pham-chi-tiet")
    public ResponseEntity<?> getProductDetails(ADPDProductDetailRequest request) {
        return Helper.createResponseEntity(adBanHangService.getProductDetails(request));
    }

    @PostMapping("/them-san-pham")
    public ResponseEntity<?> modifyProduct(@RequestBody ADThemSanPhamRequest request) {
        ResponseEntity<?> response = Helper.createResponseEntity(adBanHangService.createThemSanPham(request));
        sendRealtimeUpdate("{\"action\": \"UPDATE_CART\"}");
        return response;
    }

    @PostMapping("/xoa-san-pham")
    public ResponseEntity<?> xoaSanPham(@RequestBody ADXoaSanPhamRequest request) {
        ResponseEntity<?> response = Helper.createResponseEntity(adBanHangService.xoaSanPham(request));
        sendRealtimeUpdate("{\"action\": \"UPDATE_CART\"}"); // Thêm dòng này để xoá SP app tự update
        return response;
    }

    @GetMapping("/imei/{idProductDetail}")
    ResponseEntity<?> getImeiProduct(@PathVariable String idProductDetail) {
        return Helper.createResponseEntity(adBanHangService.getImeiProductDetail(idProductDetail));
    }

    @PutMapping("/gan-imei")
    public ResponseEntity<?> ganImei(@RequestBody ADGanImeiRequest request) {
        return Helper.createResponseEntity(adBanHangService.ganImei(request));
    }

    @GetMapping("/imei/{idProductDetail}/available")
    ResponseEntity<?> getImeiAvailable(@PathVariable String idProductDetail) {
        return Helper.createResponseEntity(productDetailService.getImeiAvailableForAssign(idProductDetail));
    }

    // =====================================
    // CÁC API KHÁCH HÀNG & GIAO HÀNG
    // =====================================

    @GetMapping("/list-khach-hang")
    public ResponseEntity<?> getListKhachHang(ListKhachHangRequest request) {
        return Helper.createResponseEntity(adBanHangService.listKhachHang(request));
    }

    @GetMapping("/khach-hang/{id}")
    public ADChonKhachHangResponse getKhachHang(@PathVariable("id") String id) {
        return adBanHangService.getKhachHang(id);
    }

    @PostMapping("/them-khach-hang")
    public void getListKhachHang(ADThemKhachHangRequest id) {
        adBanHangService.themKhachHang(id);
        sendRealtimeUpdate("{\"action\": \"UPDATE_CART\"}");
    }

    @PostMapping("/them-moi-khach-hang")
    public ResponseEntity<?> themMoiKhachHang(@ModelAttribute ADThemMoiKhachHangRequest request) {
        ResponseEntity<?> response = Helper.createResponseEntity(adBanHangService.themMoiKhachHang(request));
        sendRealtimeUpdate("{\"action\": \"UPDATE_CART\"}");
        return response;
    }

    @PutMapping("/bo-chon-khach-hang/{idHoaDon}")
    public ResponseEntity<?> boChonKhachHang(@PathVariable String idHoaDon) {
        ResponseEntity<?> response = Helper.createResponseEntity(adBanHangService.boChonKhachHang(idHoaDon));
        sendRealtimeUpdate("{\"action\": \"UPDATE_CART\"}");
        return response;
    }

    @PostMapping("/giao-hang/{id}")
    public ResponseEntity<?> getGiaoHang(@PathVariable("id") String id) {
        return Helper.createResponseEntity(adBanHangService.giaoHang(id));
    }

    // =====================================
    // VOUCHER & THANH TOÁN
    // =====================================

    @PostMapping("/goi-y")
    public ResponseEntity<VoucherSuggestionResponse> goiY(@RequestBody VoucherSuggestionRequest req) {
        return ResponseEntity.ok(adBanHangService.goiYVoucher(req));
    }

    @GetMapping("/phuong-thuc-thanh-toan/{id}")
    public List<ADPhuongThucThanhToanRespones> getPhuongThucThanhToan(@PathVariable("id") String id) {
        return adBanHangService.getPhuongThucThanhToan(id);
    }

    // =====================================
    // CÁC API THUỘC TÍNH SẢN PHẨM (COMBOBOX)
    // =====================================

    @GetMapping("/screens")
    ResponseEntity<?> getScreens() { return Helper.createResponseEntity(adBanHangService.getScreens()); }

    @GetMapping("/batteries")
    ResponseEntity<?> getBatteries() { return Helper.createResponseEntity(adBanHangService.getBatteries()); }

    @GetMapping("/operating-systems")
    ResponseEntity<?> getOperatingSystems() { return Helper.createResponseEntity(adBanHangService.getOperatingSystems()); }

    @GetMapping("/brands")
    ResponseEntity<?> getBrands() { return Helper.createResponseEntity(adBanHangService.getBrands()); }

    @GetMapping("/colors")
    ResponseEntity<?> getColors() { return Helper.createResponseEntity(adBanHangService.getColors()); }

    @GetMapping("/rams")
    ResponseEntity<?> getRAMs() { return Helper.createResponseEntity(adBanHangService.getRAMs()); }

    @GetMapping("/cpus")
    ResponseEntity<?> getCPUs() { return Helper.createResponseEntity(adBanHangService.getCPUs()); }

    @GetMapping("/hard-drives")
    ResponseEntity<?> getHardDrives() { return Helper.createResponseEntity(adBanHangService.getHardDrivers()); }

    @GetMapping("/materials")
    ResponseEntity<?> getMaterials() { return Helper.createResponseEntity(adBanHangService.getMaterials()); }

    @GetMapping("/gpus")
    ResponseEntity<?> getGPUs() {
        return Helper.createResponseEntity(adBanHangService.getGPUs());
    }

    @PostMapping("/shipping-fee")
    public ResponseEntity<?> calculateShippingFee(@RequestBody ShippingFeeRequest req) {
        log.info("Shipping fee request: province='{}', ward='{}'",
                req.getProvinceName(), req.getWardName());
        try {
            ShippingFeeResponse fee = shippingFeeService.calculateFee(req);
            return ResponseEntity.ok(fee);
        } catch (Exception e) {
            log.error("Shipping fee error: {}", e.getMessage());
            return ResponseEntity.ok(ShippingFeeResponse.builder()
                    .fee(0)
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
