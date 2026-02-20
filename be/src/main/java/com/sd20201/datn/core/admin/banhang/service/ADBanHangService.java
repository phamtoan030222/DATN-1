package com.sd20201.datn.core.admin.banhang.service;

import com.sd20201.datn.core.admin.banhang.model.request.*;
import com.sd20201.datn.core.admin.banhang.model.response.*;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Voucher;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface ADBanHangService {
    List<ListHoaDon> getHoaDon();

    ResponseObject<?> themMoiKhachHang(ADThemMoiKhachHangRequest request);

    ResponseObject<?> getProductDetails(ADPDProductDetailRequest request);

    ResponseObject<?> createHoaDon(ADNhanVienRequest adNhanVienRequest);

    List<ADGioHangResponse> getListGioHang(String id);

    ResponseObject<?> listKhachHang(ListKhachHangRequest request );

    ADChonKhachHangResponse getKhachHang(String id);

    List<ADPhuongThucThanhToanRespones> getPhuongThucThanhToan(String id);

    ResponseObject<?> createThemSanPham(ADThemSanPhamRequest adThemSanPhamRequest);

    ResponseObject<?> thanhToanThanhCong(ADThanhToanRequest id) throws BadRequestException;

    List<Voucher> danhSachPhieuGiamGia1(ChonPhieuGiamGiaRequest id) throws BadRequestException;

    void themKhachHang(ADThemKhachHangRequest request);

    ResponseObject<?> giaoHang(String request);


    ResponseObject<?> huy(ADHuyRequest request);


    VoucherSuggestionResponse goiYVoucher(
            VoucherSuggestionRequest req
    );

    ResponseObject<?> getScreens();

    ResponseObject<?> getBrands();

    ResponseObject<?> getBatteries();

    ResponseObject<?> getOperatingSystems();

    ResponseObject<?> getColors();

    ResponseObject<?> getRAMs();

    ResponseObject<?> getCPUs();

    ResponseObject<?> getHardDrivers();

    ResponseObject<?> getGPUs();

    ResponseObject<?> getMaterials();
}
