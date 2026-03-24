package com.sd20201.datn.core.admin.usercenter.service.impl;

import com.sd20201.datn.core.admin.usercenter.model.request.DoiMatKhauRequest;
import com.sd20201.datn.core.admin.usercenter.repository.UserCenterRepository;
import com.sd20201.datn.core.admin.usercenter.service.UserCenterService;
import com.sd20201.datn.entity.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCenterServiceImpl implements UserCenterService {

    // Inject Repository của Account
    private final UserCenterRepository userCenterRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void doiMatKhau(String username, DoiMatKhauRequest request) {

        // 1. Tìm tài khoản (Account) theo username
        Account account = userCenterRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        // 2. So sánh mật khẩu cũ lấy từ object account
        if (!passwordEncoder.matches(request.getMatKhauCu(), account.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác!");
        }

        // 3. Băm mật khẩu mới và set lại vào account
        String matKhauMoiDaBam = passwordEncoder.encode(request.getMatKhauMoi());
        account.setPassword(matKhauMoiDaBam);

        // 4. Lưu lại xuống database
        userCenterRepository.save(account);
    }
}