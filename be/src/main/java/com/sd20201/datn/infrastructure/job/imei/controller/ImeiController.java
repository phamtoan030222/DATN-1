package com.sd20201.datn.infrastructure.job.imei.controller;

import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.infrastructure.job.common.model.request.EXDataRequest;
import com.sd20201.datn.infrastructure.job.imei.service.IMEIExcelService;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_DETAIL + "/imei")
@RequiredArgsConstructor
public class ImeiController {
//
//    private final IMEIExcelService imeiExcelService;
//
//    @GetMapping("/download-template")
//    public ResponseEntity<?> downloadTemplate(EXDataRequest request) {
//        return imeiExcelService.downloadTemplate(request);
//    }
//
//    @PostMapping("/import")
//    public ResponseEntity<?> importTemplate(@RequestParam("file") MultipartFile file) {
//        return Helper.createResponseEntity(imeiExcelService.importExcelIMEI(file));
//    }
}
