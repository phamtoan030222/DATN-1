package com.sd20201.datn.core.admin.staff.service.impl;

import com.sd20201.datn.core.admin.staff.model.request.ADCreateStaff;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import com.sd20201.datn.infrastructure.email.EmailService;
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.repository.StaffRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private EmailService emailService;

    private String generateUsername(String fullName) {
        return fullName.trim().toLowerCase().replaceAll("\\s+", ".") + (new Random().nextInt(9000) + 1000);
    }

    private String generatePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#&!";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
        return sb.toString();
    }

    private String generateStaffCode(RoleConstant role) {
        String prefix = switch (role) {
            case NHAN_VIEN -> "NV";
            case QUAN_LY -> "QL";
            case KHACH_HANG -> "KH";
        };
        return prefix + (new Random().nextInt(9000) + 1000);
    }

    @Transactional
    public ResponseObject<?> createStaff(ADCreateStaff request) {
        RoleConstant roleEnum = RoleConstant.valueOf(request.getRole());
        String username = generateUsername(request.getFullName());
        String password = generatePassword();
        String staffCode = generateStaffCode(roleEnum);

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(password);
        account.setRoleConstant(roleEnum);
        account = accountRepository.save(account);

        Staff staff = new Staff();
        staff.setAccount(account);
        staff.setName(request.getFullName());
        staff.setCode(staffCode);
        staff.setCitizenIdentifyCard(request.getCitizenIdentifyCard());
        staff.setBirthday(request.getBirthday());
        staff.setHometown(request.getHometown());
        staff.setGender(request.getGender());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        staff.setAvatarUrl(request.getAvatarUrl());
        staff.setProvinceCode(request.getProvinceCode());
        staff.setDistrictCode(request.getDistrictCode());
        staff.setCommuneCode(request.getCommuneCode());
        staff = staffRepository.save(staff);

        // 🔹 Gửi email sau khi transaction commit
        try {
            emailService.sendNewStaffEmail(
                    request.getEmail(),
                    request.getFullName(),
                    staffCode,
                    username,
                    password
            );
        } catch (MessagingException e) {

            e.printStackTrace();
        }

        return new ResponseObject<>(null, HttpStatus.OK, "Thêm thành công nhân viên", true, null);
    }



}

