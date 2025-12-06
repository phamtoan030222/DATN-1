package com.sd20201.datn.core.admin.discounts.discountDetail.service;

import com.sd20201.datn.core.admin.discounts.discountDetail.model.request.AdCreateDiscountMultiRequest;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.request.AdCreateDiscountRequest;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdDiscountDetailRespone;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdProductDetailResponse;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdProductRespone;
import com.sd20201.datn.core.common.base.ResponseObject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdDiscountDetailService {
    ResponseObject<?> creatDiscount(@Valid AdCreateDiscountRequest request);

    ResponseObject<?> creatDiscountMulti(AdCreateDiscountMultiRequest request);

    ResponseObject<?> updateStatus(String id);

    ResponseObject<Page<AdDiscountDetailRespone>> getAppliedProductsByDiscountId(String discountId, Pageable pageable);

    ResponseObject<Page<AdProductDetailResponse>>  getNotAppliedProducts(String discountId, Pageable pageable);

    ResponseObject<Page<AdProductRespone>> getAllProducts(Pageable pageable);


    ResponseObject<List<AdProductDetailResponse>> getProductDetailsByProductId(String productId);
}
