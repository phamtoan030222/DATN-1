package com.sd20201.datn.core.admin.hoadon.service.impl;

import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonDetailRequest;
import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonSearchRequest;
import com.sd20201.datn.core.admin.hoadon.model.response.ADHoaDonChiTietResponseDetail;
import com.sd20201.datn.core.admin.hoadon.model.response.HoaDonPageResponse;
import com.sd20201.datn.core.admin.hoadon.repository.ADHoaDonChiTietRepository;
import com.sd20201.datn.core.admin.hoadon.repository.ADInvoiceRepository;
import com.sd20201.datn.core.admin.hoadon.service.ADHoaDonService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ADHoaDonServiceImpl implements ADHoaDonService {

    public final ADInvoiceRepository adHoaDonRepository;

    public final ADHoaDonChiTietRepository adHoaDonChiTietRepository;


    @Override
    public ResponseObject<?> getAllHoaDon(ADHoaDonSearchRequest request) {
        try {
            Pageable pageable = Helper.createPageable(request, "created_date");
            HoaDonPageResponse result = adHoaDonRepository.getAllHoaDonResponse(request, pageable);

            return new ResponseObject<>(
                    result,
                    HttpStatus.OK,
                    "Lấy danh sách hóa đơn thành công"
            );
        } catch (Exception e) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy danh sách hóa đơn: " + e.getMessage()
            );
        }
    }

    @Override
    public ResponseObject<?> getAllHoaDonCT(ADHoaDonDetailRequest request) {
        try {
            Pageable pageable = Helper.createPageable(request, "created_date");
            Page<ADHoaDonChiTietResponseDetail> page = adHoaDonChiTietRepository.getAllHoaDonChiTietResponse(request.getMaHoaDon(), pageable);
            return new ResponseObject<>(
                    page,
                    HttpStatus.OK,
                    "Lấy danh sách chi tiết hóa đơn thành công"
            );
        } catch (Exception e) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy chi tiết hóa đơn: " + e.getMessage()
            );
        }
    }

    @Override
    public ResponseObject<?> getAllHoaDonCT1(ADHoaDonDetailRequest request) {
        try {
            List<ADHoaDonChiTietResponseDetail> page = adHoaDonChiTietRepository.getAllHoaDonChiTietResponse2(request.getMaHoaDon());
            return new ResponseObject<>(
                    page,
                    HttpStatus.OK,
                    "Lấy danh sách chi tiết hóa đơn thành công"
            );
        } catch (Exception e) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy chi tiết hóa đơn: " + e.getMessage()
            );
        }
    }

}
