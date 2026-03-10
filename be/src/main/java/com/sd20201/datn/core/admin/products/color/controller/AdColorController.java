package com.sd20201.datn.core.admin.products.color.controller;

import com.sd20201.datn.core.admin.products.brand.model.request.ADCreateBrandRequest;
import com.sd20201.datn.core.admin.products.color.model.request.AdColorRequest;
import com.sd20201.datn.core.admin.products.color.model.request.ColorCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.color.service.AdColorService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_COLOR)
@RequiredArgsConstructor
public class AdColorController {

    private final AdColorService adColorService;

    @GetMapping
    public ResponseEntity<?> getALlColor(@ModelAttribute AdColorRequest request){
        return Helper.createResponseEntity(adColorService.getAllColors(request));
    }

    @PostMapping("/add")
    public ResponseEntity<?> cteateColor(@RequestBody ColorCreateUpdateRequest request){
        return Helper.createResponseEntity(adColorService.createColor(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateColor(@PathVariable String id, @RequestBody ColorCreateUpdateRequest request) {
        return Helper.createResponseEntity(adColorService.updateColor(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateColorStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adColorService.updateColorStatus(id));
    }
}
