package com.sd20201.datn.core.admin.discounts.discount.controller;

import com.sd20201.datn.core.admin.discounts.discount.model.request.AdDiscountRequest;
import com.sd20201.datn.core.admin.discounts.discount.model.request.AdDscountFilterRequest;
import com.sd20201.datn.core.admin.discounts.discount.model.request.DiscountUpdateRequest;
import com.sd20201.datn.core.admin.discounts.discount.model.request.DiscountValidateRequest;
import com.sd20201.datn.core.admin.discounts.discount.service.AdDiscountService;
import com.sd20201.datn.core.admin.discounts.discount.service.MailService;
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
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_DISCOUNT_DISCOUNT)
@RequiredArgsConstructor
public class AdDiscountController {
     @Autowired
     private final MailService mailService;

    private final AdDiscountService adDiscountService;
    @GetMapping()
    public ResponseEntity<?> getALLDiscount(@ModelAttribute AdDiscountRequest request){
        return Helper.createResponseEntity(adDiscountService.getAllDiscounts(request));
    }

    @PostMapping("/addDiscount")
    public ResponseEntity<?> addDiscount(@Valid @RequestBody DiscountValidateRequest request){
        return Helper.createResponseEntity(adDiscountService.creatDiscount(request));
    }


    @PutMapping("/updateDiscount/{id}")
    public ResponseEntity<?> updateDiscount( @PathVariable("id") String id ,@Valid @RequestBody DiscountUpdateRequest request){
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
    public ResponseEntity<?> filterDiscount(@RequestBody  AdDscountFilterRequest request) {
        return Helper.createResponseEntity(adDiscountService.filterDiscounts(request));
    }

    @PostMapping("/sendEmail/{id}")
    public ResponseEntity<?> sendMail(@PathVariable String id){
        return Helper.createResponseEntity(adDiscountService.sendEmailDiscount(id));
    }

}
