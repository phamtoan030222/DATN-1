package com.sd20201.datn.core.client.banhang.service;

import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientNhanVienRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThanhToanRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThemKhachHangRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThemSanPhamRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientChonPhieuGiamGiaRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientListKhachHangRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientVoucherSuggestionRequest;
import com.sd20201.datn.core.client.banhang.model.response.ClientChonKhachHangResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientGioHangResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientPhuongThucThanhToanRespones;
import com.sd20201.datn.core.client.banhang.model.response.ClientListHoaDon;
import com.sd20201.datn.core.client.banhang.model.response.ClientVoucherSuggestionResponse;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Voucher;
import org.apache.coyote.BadRequestException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ClientBanHangService {
    List<ClientListHoaDon> getHoaDon();

    @Transactional
    ResponseObject<?> createOrder(ClientThanhToanRequest request);

    ResponseObject<?> getProductDetails(ADPDProductDetailRequest request);

    ResponseObject<?> createHoaDon(ClientNhanVienRequest adNhanVienRequest);

    List<ClientGioHangResponse> getListGioHang(String id);

    ResponseObject<?> listKhachHang(ClientListKhachHangRequest request);

    ClientChonKhachHangResponse getKhachHang(String id);

    List<ClientPhuongThucThanhToanRespones> getPhuongThucThanhToan(String id);

    ResponseObject<?> createThemSanPham(ClientThemSanPhamRequest adThemSanPhamRequest);

    ResponseObject<?> thanhToanThanhCong(ClientThanhToanRequest id) throws BadRequestException;

    List<Voucher> danhSachPhieuGiamGia1(ClientChonPhieuGiamGiaRequest id) throws BadRequestException;

    void themKhachHang(ClientThemKhachHangRequest request);


    ClientVoucherSuggestionResponse goiYVoucher(
            ClientVoucherSuggestionRequest req
    );

    ResponseObject<?> xoaSanPhamKhoiGioHang(String idHDCT);

    // TĂNG SỐ LƯỢNG: Tự động lấy thêm 1 IMEI Available bất kỳ của sản phẩm đó
    @Transactional
    ResponseObject<?> tangSoLuongSanPham(String idHDCT);

    // GIẢM SỐ LƯỢNG: Bỏ bớt 1 IMEI ra khỏi giỏ
    @Transactional
    ResponseObject<?> giamSoLuongSanPham(String idHDCT);
}
