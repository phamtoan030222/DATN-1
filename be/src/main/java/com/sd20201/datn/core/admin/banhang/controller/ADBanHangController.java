package com.sd20201.datn.core.admin.banhang.controller;

import com.sd20201.datn.core.admin.banhang.model.request.*;
import com.sd20201.datn.core.admin.banhang.model.response.ADChonKhachHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADGioHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADPhuongThucThanhToanRespones;
import com.sd20201.datn.core.admin.banhang.model.response.ListHoaDon;
import com.sd20201.datn.core.admin.banhang.model.response.VoucherSuggestionResponse;
import com.sd20201.datn.core.admin.banhang.service.ADBanHangService;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDImeiResponse;
import com.sd20201.datn.core.admin.products.productdetail.service.ADProductDetailService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_ADMIN_BAN_HANG)
@Slf4j
@CrossOrigin(origins = "*")
public class ADBanHangController {
    private final ADProductDetailService productDetailService;

    public final ADBanHangService adBanHangService;

    @GetMapping("/san-pham-chi-tiet")
    public ResponseEntity<?> getProductDetails(ADPDProductDetailRequest request) {
        return Helper.createResponseEntity(adBanHangService.getProductDetails(request));
    }

    @GetMapping("/list-hoa-don")
    public List<ListHoaDon> getListHoaDon() {
        return adBanHangService.getHoaDon();
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

        @GetMapping("/imei/{idProductDetail}")
    ResponseEntity<?> getImeiProduct(@PathVariable String idProductDetail) {
        return Helper.createResponseEntity(productDetailService.getImeiProductDetail(idProductDetail));
    }


    @GetMapping("/phuong-thuc-thanh-toan/{id}")
    public  List<ADPhuongThucThanhToanRespones>  getPhuongThucThanhToan(@PathVariable("id") String id) {
        return adBanHangService.getPhuongThucThanhToan(id);
    }

    @PostMapping("them-san-pham")
    public ResponseEntity<?> modifyProduct(@RequestBody ADThemSanPhamRequest request) {
        return Helper.createResponseEntity(adBanHangService.createThemSanPham(request));
    }

    @PostMapping("/thanh-toan-thanh-cong")
    public ResponseEntity<?>  thanhToanThanhCong(@RequestBody ADThanhToanRequest id) throws BadRequestException {
        return Helper.createResponseEntity(adBanHangService.thanhToanThanhCong(id));
    }

    @PostMapping("/them-khach-hang")
    public void getListKhachHang(ADThemKhachHangRequest id) {
        adBanHangService.themKhachHang(id);
    }

    @PostMapping("/them-moi-khach-hang")
    public ResponseEntity<?> themMoiKhachHang(@ModelAttribute ADThemMoiKhachHangRequest request) {return Helper.createResponseEntity(adBanHangService.themMoiKhachHang(request));}

    @PostMapping("/goi-y")
    public ResponseEntity<VoucherSuggestionResponse> goiY(
            @RequestBody VoucherSuggestionRequest req
    ) {
        return ResponseEntity.ok(adBanHangService.goiYVoucher(req));
    }

    @PostMapping("/giao-hang/{id}")
    public ResponseEntity<?> getGiaoHang(@PathVariable("id") String id) {
        return Helper.createResponseEntity(adBanHangService.giaoHang(id));

    }

    @PostMapping("/huy")
    public ResponseEntity<?> huyHoaDon(ADHuyRequest adNhanVienRequest) {
        return Helper.createResponseEntity(adBanHangService.huy(adNhanVienRequest));
    }

    @GetMapping("/screens")
    ResponseEntity<?> getScreens() {
        return Helper.createResponseEntity(adBanHangService.getScreens());
    }

    @GetMapping("/batteries")
    ResponseEntity<?> getBatteries() {
        return Helper.createResponseEntity(adBanHangService.getBatteries());
    }

    @GetMapping("/operating-systems")
    ResponseEntity<?> getOperatingSystems() {
        return Helper.createResponseEntity(adBanHangService.getOperatingSystems());
    }

    @GetMapping("/brands")
    ResponseEntity<?> getBrands() {
        return Helper.createResponseEntity(adBanHangService.getBrands());
    }

    @GetMapping("/colors")
    ResponseEntity<?> getColors() {
        return Helper.createResponseEntity(adBanHangService.getColors());
    }

    @GetMapping("/rams")
    ResponseEntity<?> getRAMs() {
        return Helper.createResponseEntity(adBanHangService.getRAMs());
    }

    @GetMapping("/cpus")
    ResponseEntity<?> getCPUs() {
        return Helper.createResponseEntity(adBanHangService.getCPUs());
    }

    @GetMapping("/hard-drives")
    ResponseEntity<?> getHardDrives() {
        return Helper.createResponseEntity(adBanHangService.getHardDrivers());
    }

    @GetMapping("/materials")
    ResponseEntity<?> getMaterials() {
        return Helper.createResponseEntity(adBanHangService.getMaterials());
    }

    @GetMapping("/gpus")
    ResponseEntity<?> getGPUs() {
        return Helper.createResponseEntity(adBanHangService.getGPUs());
    }

}
