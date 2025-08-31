package com.sd20201.datn.core.admin.discounts.discount.service;

import com.sd20201.datn.core.admin.discounts.discount.model.request.AdDiscountRequest;
import com.sd20201.datn.core.admin.discounts.discount.model.request.AdDscountFilterRequest;
import com.sd20201.datn.core.admin.discounts.discount.model.request.DiscountUpdateRequest;
import com.sd20201.datn.core.admin.discounts.discount.model.request.DiscountValidateRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import jakarta.validation.Valid;

public interface AdDiscountService {
    ResponseObject<?> getAllDiscounts(AdDiscountRequest request);

    ResponseObject<?> creatDiscount(@Valid DiscountValidateRequest request);

    ResponseObject<?> updateDiscount(@Valid  String id,  DiscountUpdateRequest request);

    ResponseObject<?> endDiscount(String id);

    ResponseObject<?> startDiscount(String id);

    ResponseObject<?> deleteDiscount(String id);

    ResponseObject<?> filterDiscounts(AdDscountFilterRequest request);

}
