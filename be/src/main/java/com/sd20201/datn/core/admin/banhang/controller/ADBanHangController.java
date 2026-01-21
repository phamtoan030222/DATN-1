package com.sd20201.datn.core.admin.banhang.controller;

import com.sd20201.datn.core.admin.banhang.model.request.*;
import com.sd20201.datn.core.admin.banhang.model.response.*;
import com.sd20201.datn.core.admin.banhang.service.ADBanHangService;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
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

    public final ADBanHangService adBanHangService;

<<<<<<< HEAD
    @GetMapping("/san-pham-chi-tiet")
    public ResponseEntity<?> getProductDetails(ADPDProductDetailRequest request) {
        return Helper.createResponseEntity(adBanHangService.getProductDetails(request));
    }

=======
   @Autowired   
    private PhieuGiamGiaService phieuGiamGiaService;
>>>>>>> d72cc722b3c5f5b35c40bf64646a23150eeb89d9

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

<<<<<<< HEAD
    @PostMapping("/them-khach-hang")
    public void getListKhachHang(ADThemKhachHangRequest id) {
        adBanHangService.themKhachHang(id);
    }

    @PostMapping("/goi-y")
    public ResponseEntity<VoucherSuggestionResponse> goiY(
            @RequestBody VoucherSuggestionRequest req
    ) {
        return ResponseEntity.ok(adBanHangService.goiYVoucher(req));
    }


=======
>>>>>>> d72cc722b3c5f5b35c40bf64646a23150eeb89d9
}
