package com.sd20201.datn.core.client.discounts.discountDetail.service;

import com.sd20201.datn.core.client.discounts.discountDetail.model.request.ClientCreateDiscountMultiRequest;
import com.sd20201.datn.core.client.discounts.discountDetail.model.request.ClientCreateDiscountRequest;
import com.sd20201.datn.core.client.discounts.discountDetail.model.respone.ClientDiscountDetailRespone;
import com.sd20201.datn.core.client.discounts.discountDetail.model.respone.ClientProductDetailResponse;
import com.sd20201.datn.core.client.discounts.discountDetail.model.respone.ClientProductRespone;
import com.sd20201.datn.core.common.base.ResponseObject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClientDiscountDetailService {
    ResponseObject<?> creatDiscount(@Valid ClientCreateDiscountRequest request);

    ResponseObject<?> creatDiscountMulti(ClientCreateDiscountMultiRequest request);

    ResponseObject<?> updateStatus(String id);

    ResponseObject<Page<ClientDiscountDetailRespone>> getAppliedProductsByDiscountId(String discountId, Pageable pageable);

    ResponseObject<Page<ClientProductDetailResponse>>  getNotAppliedProducts(String discountId, Pageable pageable);

    ResponseObject<Page<ClientProductRespone>> getAllProducts(Pageable pageable);


    ResponseObject<List<ClientProductDetailResponse>> getProductDetailsByProductId(String productId);
}
