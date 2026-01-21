package com.sd20201.datn.core.admin.banhang.controller;

import com.sd20201.datn.core.admin.banhang.model.request.*;
import com.sd20201.datn.core.admin.banhang.model.response.*;
import com.sd20201.datn.core.admin.banhang.service.ADBanHangService;
import com.sd20201.datn.core.admin.banhang.service.impl.PhieuGiamGiaService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_ADMIN_BAN_HANG)
@Slf4j
@CrossOrigin(origins = "*")
public class ADBanHangController {

    public final ADBanHangService adBanHangService;

   @Autowired   
    private PhieuGiamGiaService phieuGiamGiaService;

    @GetMapping("/list-hoa-don")
    public List<ListHoaDon> getListHoaDon() {
        return adBanHangService.getHoaDon();
    }

    @GetMapping("/danh-sach-phieu-giam-gia-ko_du")
    public ResponseEntity<AvailableVouchersResponse> getAvailableVouchers (ChonPhieuGiamGiaKoDuRequest chonPhieuGiamGiaKoDuRequest) {
        AvailableVouchersResponse response = phieuGiamGiaService.getAvailableVouchers(chonPhieuGiamGiaKoDuRequest.getIdHD(), chonPhieuGiamGiaKoDuRequest.getIdKH(), chonPhieuGiamGiaKoDuRequest.getTongTien());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-hoa-don")
    public ResponseEntity<?> createHoaDon(@RequestBody ADNhanVienRequest adNhanVienRequest) {
        return Helper.createResponseEntity(adBanHangService.createHoaDon(adNhanVienRequest));
    }

    @GetMapping("/list-gio-hang/{id}")
    public List<ADGioHangResponse> getListGioHang(@PathVariable("id") String id) {
        return adBanHangService.getListGioHang(id);
    }

    @GetMapping("/list-khach-hang")
    public ResponseEntity<?> getListKhachHang(ListKhachHangRequest request) {
        return Helper.createResponseEntity(adBanHangService.listKhachHang(request));
    }

    @GetMapping("/khach-hang/{id}")
    public ADChonKhachHangResponse getKhachHang(@PathVariable("id") String id) {
        return adBanHangService.getKhachHang(id);
    }


    @GetMapping("/danh-sach-phieu-giam-gia")
    public ResponseEntity<?> getDiscountCoupons(ChonPhieuGiamGiaRequest request) {
        return Helper.createResponseEntity(adBanHangService.danhSachPhieuGiamGia(request));

    }


    @GetMapping("/phuong-thuc-thanh-toan/{id}")
    public  List<ADPhuongThucThanhToanRespones>  getPhuongThucThanhToan(@PathVariable("id") String id) {
        return adBanHangService.getPhuongThucThanhToan(id);
    }

    @PostMapping("them-san-pham")
    public ResponseEntity<?> modifyProduct(@ModelAttribute ADThemSanPhamRequest request) {
        return Helper.createResponseEntity(adBanHangService.createThemSanPham(request));
    }

    @PostMapping("/thanh-toan-thanh-cong")
    public ResponseEntity<?>  thanhToanThanhCong(@RequestBody ADThanhToanRequest id) throws BadRequestException {
        return Helper.createResponseEntity(adBanHangService.thanhToanThanhCong(id));
    }

}
