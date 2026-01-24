package com.sd20201.datn.core.admin.hoadon.repository;

import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonSearchRequest;
import com.sd20201.datn.core.admin.hoadon.model.response.HoaDonPageResponse;
import org.springframework.data.domain.Pageable;


public interface ADHoaDonRepositoryCustom {
    HoaDonPageResponse getAllHoaDonResponse(ADHoaDonSearchRequest request, Pageable pageable);


}