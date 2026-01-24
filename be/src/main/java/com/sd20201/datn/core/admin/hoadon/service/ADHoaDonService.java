package com.sd20201.datn.core.admin.hoadon.service;


import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonDetailRequest;
import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonSearchRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADHoaDonService {

    ResponseObject<?> getAllHoaDon(ADHoaDonSearchRequest request);

    ResponseObject<?> getAllHoaDonCT(ADHoaDonDetailRequest request);

    ResponseObject<?> getAllHoaDonCT1(ADHoaDonDetailRequest request);

}
