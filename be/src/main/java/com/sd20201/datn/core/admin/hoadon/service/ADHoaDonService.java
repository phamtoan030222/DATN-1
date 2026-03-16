package com.sd20201.datn.core.admin.hoadon.service;


import com.sd20201.datn.core.admin.hoadon.model.request.*;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADHoaDonService {

    ResponseObject<?> getAllHoaDon(ADHoaDonSearchRequest request);

    ResponseObject<?> getAllHoaDonCT(ADHoaDonDetailRequest request);

    ResponseObject<?> capNhatTrangThaiHoaDon(ADChangeStatusRequest adChangeStatusRequest);

    ResponseObject<?> doiImei(ADDoiImeiRequest request);

    ResponseObject<?> capNhatThongTinKhachHang(ADCapNhatKhachHangRequest request);

    ResponseObject<?> hoanPhi(ADHoanPhiRequest request);

}
