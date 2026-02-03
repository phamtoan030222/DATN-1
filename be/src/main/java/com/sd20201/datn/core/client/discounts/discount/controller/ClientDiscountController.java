package com.sd20201.datn.core.client.discounts.discount.controller;

import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDscountFilterRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountUpdateRequest;
import com.sd20201.datn.core.client.discounts.discount.model.request.ClientDiscountValidateRequest;
import com.sd20201.datn.core.client.discounts.discount.service.ClientDiscountService;
import com.sd20201.datn.core.client.discounts.discount.service.ClientMailService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_CUSTOMER_PREFIX_DISCOUNT_DISCOUNT)
@RequiredArgsConstructor
public class ClientDiscountController {
     @Autowired
     private final ClientMailService mailService;

    private final ClientDiscountService adDiscountService;
    @GetMapping()
    public ResponseEntity<?> getALLDiscount(@ModelAttribute ClientDiscountRequest request){
        return Helper.createResponseEntity(adDiscountService.getAllDiscounts(request));
    }

    @PostMapping("/addDiscount")
    public ResponseEntity<?> addDiscount(@Valid @RequestBody ClientDiscountValidateRequest request){
        return Helper.createResponseEntity(adDiscountService.creatDiscount(request));
    }


    @PutMapping("/updateDiscount/{id}")
    public ResponseEntity<?> updateDiscount( @PathVariable("id") String id ,@Valid @RequestBody ClientDiscountUpdateRequest request){
        return Helper.createResponseEntity(adDiscountService.updateDiscount(id,request));
    }

    @PutMapping("/end/{id}")
    public ResponseEntity<?>endDiscount(@PathVariable String id) {
        return Helper.createResponseEntity(adDiscountService.endDiscount(id));
    }

    @PutMapping("/start/{id}")
    public ResponseEntity<?>startDiscount(@PathVariable String id) {
        return Helper.createResponseEntity(adDiscountService.startDiscount(id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return Helper.createResponseEntity(adDiscountService.deleteDiscount(id));
    }

    @PostMapping("/filter")
    public ResponseEntity<?> filterDiscount(@RequestBody ClientDscountFilterRequest request) {
        return Helper.createResponseEntity(adDiscountService.filterDiscounts(request));
    }

}
