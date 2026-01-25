package com.sd20201.datn.core.client.banhang.controller;

import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientNhanVienRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThanhToanRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThemKhachHangRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThemSanPhamRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientListKhachHangRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientVoucherSuggestionRequest;
import com.sd20201.datn.core.client.banhang.model.response.ClientChonKhachHangResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientGioHangResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientPhuongThucThanhToanRespones;
import com.sd20201.datn.core.client.banhang.model.response.ClientListHoaDon;
import com.sd20201.datn.core.client.banhang.model.response.ClientVoucherSuggestionResponse;
import com.sd20201.datn.core.client.banhang.service.ClientBanHangService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_CUSTOMER_BAN_HANG)
@Slf4j
@CrossOrigin(origins = "*")
public class ClientBanHangController {

    public final ClientBanHangService adBanHangService;

    @GetMapping("/san-pham-chi-tiet")
    public ResponseEntity<?> getProductDetails(ADPDProductDetailRequest request) {
        return Helper.createResponseEntity(adBanHangService.getProductDetails(request));
    }


    @GetMapping("/list-hoa-don")
    public List<ClientListHoaDon> getListHoaDon() {
        return adBanHangService.getHoaDon();
    }


    @PostMapping("/create-hoa-don")
    public ResponseEntity<?> createHoaDon(@RequestBody ClientNhanVienRequest adNhanVienRequest) {
        return Helper.createResponseEntity(adBanHangService.createHoaDon(adNhanVienRequest));
    }

    @GetMapping("/list-gio-hang/{id}")
    public List<ClientGioHangResponse> getListGioHang(@PathVariable("id") String id) {
        return adBanHangService.getListGioHang(id);
    }

    @GetMapping("/list-khach-hang")
    public ResponseEntity<?> getListKhachHang(ClientListKhachHangRequest request) {
        return Helper.createResponseEntity(adBanHangService.listKhachHang(request));
    }

    @GetMapping("/khach-hang/{id}")
    public ClientChonKhachHangResponse getKhachHang(@PathVariable("id") String id) {
        return adBanHangService.getKhachHang(id);
    }


    @GetMapping("/phuong-thuc-thanh-toan/{id}")
    public  List<ClientPhuongThucThanhToanRespones>  getPhuongThucThanhToan(@PathVariable("id") String id) {
        return adBanHangService.getPhuongThucThanhToan(id);
    }

    @PostMapping("them-san-pham")
    public ResponseEntity<?> modifyProduct(@RequestBody ClientThemSanPhamRequest request) {
        return Helper.createResponseEntity(adBanHangService.createThemSanPham(request));
    }

    @PostMapping("/thanh-toan-thanh-cong")
    public ResponseEntity<?>  thanhToanThanhCong(@RequestBody ClientThanhToanRequest id) throws BadRequestException {
        return Helper.createResponseEntity(adBanHangService.thanhToanThanhCong(id));
    }

    @PostMapping("/them-khach-hang")
    public void getListKhachHang(ClientThemKhachHangRequest id) {
        adBanHangService.themKhachHang(id);
    }

    @PostMapping("/goi-y")
    public ResponseEntity<ClientVoucherSuggestionResponse> goiY(
            @RequestBody ClientVoucherSuggestionRequest req
    ) {
        return ResponseEntity.ok(adBanHangService.goiYVoucher(req));
    }

}
