package com.sd20201.datn.core.admin.products.harddrive.controller;

import com.sd20201.datn.core.admin.products.harddrive.model.request.ADCreateHardDriverRequest;
import com.sd20201.datn.core.admin.products.harddrive.model.request.ADHardDriveRequest;
import com.sd20201.datn.core.admin.products.harddrive.service.ADHardDriveService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_HARDDRIVE)
@RequiredArgsConstructor
public class ADHardDriveController {

    private final ADHardDriveService adHardDriveService;

    // Lấy danh sách ổ cứng, phân trang + filter
    @GetMapping
    public ResponseEntity<?> getAllHardDrives(@ModelAttribute ADHardDriveRequest request) {
        return Helper.createResponseEntity(adHardDriveService.getAllHardDrives(request));
    }

    // Thêm mới ổ cứng
    @PostMapping("/add")
    public ResponseEntity<?> createHardDrive(@RequestBody ADCreateHardDriverRequest request) {
        return Helper.createResponseEntity(adHardDriveService.createHardDrive(request));
    }

    // Cập nhật ổ cứng theo id
    @PutMapping("/{id}")
    public ResponseEntity<?> updateHardDrive(@PathVariable String id, @RequestBody ADCreateHardDriverRequest request) {
        return Helper.createResponseEntity(adHardDriveService.updateHardDrive(id, request));
    }

    // Cập nhật trạng thái ACTIVE/INACTIVE theo id
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateHardDriveStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adHardDriveService.updateHardDriveStatus(id));
    }
}
