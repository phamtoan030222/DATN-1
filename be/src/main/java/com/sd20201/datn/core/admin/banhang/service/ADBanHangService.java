package com.sd20201.datn.core.admin.banhang.service;

import com.sd20201.datn.core.admin.banhang.model.request.*;
import com.sd20201.datn.core.admin.banhang.model.response.ADChonKhachHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADGioHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADPhuongThucThanhToanRespones;
import com.sd20201.datn.core.admin.banhang.model.response.ListHoaDon;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Voucher;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface ADBanHangService {
    List<ListHoaDon> getHoaDon();

    ResponseObject<?> createHoaDon(ADNhanVienRequest adNhanVienRequest);

    List<ADGioHangResponse> getListGioHang(String id);

    ResponseObject<?> listKhachHang(ListKhachHangRequest request );

    ADChonKhachHangResponse getKhachHang(String id);

    ResponseObject<?> danhSachPhieuGiamGia(ChonPhieuGiamGiaRequest id);

    ResponseObject<?> danhSachPhieuGiamGiaKoDuDieuKien(ChonPhieuGiamGiaRequest request);

    List<ADPhuongThucThanhToanRespones> getPhuongThucThanhToan(String id);

    ResponseObject<?> createThemSanPham(ADThemSanPhamRequest adThemSanPhamRequest);

    ResponseObject<?> thanhToanThanhCong(ADThanhToanRequest id) throws BadRequestException;

    List<Voucher> danhSachPhieuGiamGia1(ChonPhieuGiamGiaRequest id) throws BadRequestException;

}
