package com.sd20201.datn.core.client.customer.service.impl;

import com.sd20201.datn.core.client.customer.model.request.ClientChangePasswordRequest;
import com.sd20201.datn.core.client.customer.model.request.ClientCustomerUpdateInformation;
import com.sd20201.datn.core.client.customer.model.request.ForgotPasswordRequest;
import com.sd20201.datn.core.client.customer.repository.ClientCustomerRepository;
import com.sd20201.datn.core.client.customer.service.ClientCustomerService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.infrastructure.email.EmailService;
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.utils.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientCustomerServiceImpl implements ClientCustomerService {

    private final UserContextHelper userContextHelper;
    private final ClientCustomerRepository customerRepository;

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public ResponseObject<?> updateInformation(final ClientCustomerUpdateInformation request) {
        return userContextHelper.getCurrentUserId()
                .flatMap(customerRepository::findById)
                .map(customer -> {
                    customer.setPhone(request.getPhoneNumber());
                    customer.setEmail(request.getEmail());
                    customer.setName(request.getFullName());

                    // --- THÊM 2 DÒNG NÀY ĐỂ LƯU ẢNH (Kiểm tra null đề phòng khách không đổi ảnh) ---
                    if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                        customer.setAvatarUrl(request.getAvatarUrl());
                    }

                    customerRepository.save(customer);

                    return ResponseObject.successForward(customer.getId(), "OKE");
                })
                .orElse(ResponseObject.errorForward("Customer not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    @Override
    public ResponseObject<?> changePassword(String username, ClientChangePasswordRequest request) {
        // 1. Tìm Account dựa vào username (lấy từ Token)
        Account account = accountRepository.findByUsername(username)
                .orElse(null);

        if (account == null) {
            return new ResponseObject<>(null, HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!");
        }

        // 2. Kiểm tra mật khẩu cũ có khớp không bằng PasswordEncoder
        if (!passwordEncoder.matches(request.getMatKhauCu(), account.getPassword())) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác!");
        }

        // 3. Băm mật khẩu mới và lưu lại
        account.setPassword(passwordEncoder.encode(request.getMatKhauMoi()));
        accountRepository.save(account);

        return new ResponseObject<>(null, HttpStatus.OK, "Đổi mật khẩu thành công!", true, null);
    }

    // Hàm sinh mật khẩu ngẫu nhiên (8 ký tự)
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#&!";
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    @Override
    public ResponseObject<?> forgotPassword(ForgotPasswordRequest request) {
        // 1. Tìm Khách hàng dựa vào Email
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (customer == null) {
            // Trả về lỗi nếu email chưa được đăng ký
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Email này chưa được đăng ký trong hệ thống!");
        }

        // 2. Lấy Account liên kết với Customer đó
        Account account = customer.getAccount();
        if (account == null) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Tài khoản khách hàng bị lỗi, không thể cấp lại mật khẩu!");
        }

        // 3. Sinh mật khẩu mới, băm nó và lưu vào Database
        String newPlainPassword = generateRandomPassword();
        account.setPassword(passwordEncoder.encode(newPlainPassword));
        accountRepository.save(account);

        // 4. Gửi email thông báo mật khẩu mới cho khách hàng
        try {
            // Đã sửa lại thành getEmail() và getName() cho khớp với Entity của bạn
            emailService.sendResetPasswordEmail(customer.getEmail(), customer.getName(), newPlainPassword);
        } catch (Exception e) {
            e.printStackTrace();
            // Vẫn báo thành công nhưng có thể log lỗi gửi mail ra console
        }

        return new ResponseObject<>(null, HttpStatus.OK, "Mật khẩu mới đã được gửi vào email của bạn!", true, null);
    }
}