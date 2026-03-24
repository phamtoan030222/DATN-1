package com.sd20201.datn.core.admin.usercenter.controller;

import com.sd20201.datn.core.admin.usercenter.model.request.DoiMatKhauRequest;
import com.sd20201.datn.core.admin.usercenter.service.UserCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/user-center")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserCenterService userCenterService;

    @PutMapping("/doi-mat-khau")
    public ResponseEntity<?> doiMatKhau(
            @Valid @RequestBody DoiMatKhauRequest request,
            Principal principal // Đại diện cho user đang thực hiện request (lấy từ SecurityContext)
    ) {
        // principal.getName() thường sẽ trả về username hoặc email tùy cách bạn config JWT
        String username = principal.getName();

        userCenterService.doiMatKhau(username, request);

        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

}