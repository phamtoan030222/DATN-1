package com.sd20201.datn.core.admin.products.color.controller;

import com.sd20201.datn.core.admin.products.color.model.request.AdColorRequest;
import com.sd20201.datn.core.admin.products.color.model.request.ColorCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.color.service.AdColorService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    public ResponseEntity<?> updateColor(@RequestBody ColorCreateUpdateRequest request){
        return Helper.createResponseEntity(adColorService.createColor(request));
    }
}
