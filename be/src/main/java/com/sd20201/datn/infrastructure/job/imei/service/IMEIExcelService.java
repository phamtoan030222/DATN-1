package com.sd20201.datn.infrastructure.job.imei.service;

import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.infrastructure.job.common.model.request.EXDataRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IMEIExcelService {

    ResponseEntity<?> downloadTemplate(EXDataRequest request);

    ResponseObject<?> importExcelIMEI(MultipartFile file);

}
