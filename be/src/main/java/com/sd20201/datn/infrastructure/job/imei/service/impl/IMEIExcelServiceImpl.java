package com.sd20201.datn.infrastructure.job.imei.service.impl;

import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.IMEI;
import com.sd20201.datn.infrastructure.job.common.model.request.EXDataRequest;
import com.sd20201.datn.infrastructure.job.imei.model.IMEIExcelResponse;
import com.sd20201.datn.infrastructure.job.imei.repository.IMEIExcelRepository;
import com.sd20201.datn.infrastructure.job.imei.service.IMEIExcelService;
import com.sd20201.datn.utils.ExcelHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IMEIExcelServiceImpl implements IMEIExcelService {

    private final IMEIExcelRepository imeiRepository;

    @Override
    public ResponseEntity<?> downloadTemplate(EXDataRequest request) {
        String filename ="template-import-imei.xlsx";

        List<String> headers = List.of("IMEI");

        byte[] data = ExcelHelper.createExcelStream("imei-template", headers, new ArrayList<>());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
        httpHeaders.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(data, httpHeaders, HttpStatus.OK);
    }

    @Override
    public ResponseObject<?> importExcelIMEI(MultipartFile file) {
        List<String> imeis = new ArrayList<>();
        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new IllegalArgumentException("File Excel không có dữ liệu!");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("File Excel không có tiêu đề cột!");
            }

            for(int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                String imeiValue = null;
                switch (cell.getCellType()) {
                    case NUMERIC -> {
                        imeiValue = String.valueOf((int) cell.getNumericCellValue());
                    }
                    case STRING -> {
                        imeiValue = cell.getStringCellValue();
                    }
                }

                if (imeiValue != null && !imeiValue.isEmpty()) {
                    imeis.add(imeiValue);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        Set<String> imeiExist =  imeiRepository.findByNameIn(imeis).stream()
                .map(imei -> imei.getName())
                .collect(Collectors.toSet());

        return ResponseObject.successForward(
                imeis.stream()
                        .map(imei -> new IMEIExcelResponse(imei, imeiExist.contains(imei)))
                        .collect(Collectors.toList()),
                "OKE"
        );
    }
}
