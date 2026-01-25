package com.sd20201.datn.core.client.discounts.discount.service;

import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDscountFilterRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountUpdateRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountValidateRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import jakarta.validation.Valid;

public interface ClientDiscountService {
    ResponseObject<?> getAllDiscounts(ClientDiscountRequest request);

    ResponseObject<?> creatDiscount(@Valid ClientDiscountValidateRequest request);

    ResponseObject<?> updateDiscount(@Valid  String id,  ClientDiscountUpdateRequest request);

    ResponseObject<?> endDiscount(String id);

    ResponseObject<?> startDiscount(String id);

    ResponseObject<?> deleteDiscount(String id);

    ResponseObject<?> filterDiscounts(ClientDscountFilterRequest request);

    ResponseObject<?> sendEmailDiscount(String id);

}
