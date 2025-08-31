package com.sd20201.datn.core.admin.discounts.discountDetail.controller;

import com.sd20201.datn.core.admin.discounts.discountDetail.model.request.AdCreateDiscountMultiRequest;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.request.AdCreateDiscountRequest;
import com.sd20201.datn.core.admin.discounts.discountDetail.service.AdDiscountDetailService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_DISCOUNT_DETAIL)
@RequiredArgsConstructor
public class AdDiscountDetailController {
    private final AdDiscountDetailService adDiscountDetailService;

    @GetMapping("/applied-products")
    public ResponseEntity<?> getAppliedProductsByDiscount(
            @RequestParam String discountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return Helper.createResponseEntity(
                adDiscountDetailService.getAppliedProductsByDiscountId(discountId, pageable)
        );
    }

    @GetMapping("/not-applied-products")
    public ResponseEntity<?> getNotAppliedProducts(
            @RequestParam String discountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return Helper.createResponseEntity(
                adDiscountDetailService.getNotAppliedProducts(discountId, pageable)
        );
    }


    @PostMapping("/applyProduct")
    public ResponseEntity<?> applyProduct(@Valid @RequestBody AdCreateDiscountRequest request){
        return Helper.createResponseEntity(adDiscountDetailService.creatDiscount(request));
    }

    @PostMapping("/applyProducts")
    public ResponseEntity<?> applyProducts(@Valid @RequestBody AdCreateDiscountMultiRequest request) {
        return Helper.createResponseEntity(adDiscountDetailService.creatDiscountMulti(request));
    }

    @PutMapping("/updateStatus/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable String id){
        return Helper.createResponseEntity(adDiscountDetailService.updateStatus(id));
    }

}
