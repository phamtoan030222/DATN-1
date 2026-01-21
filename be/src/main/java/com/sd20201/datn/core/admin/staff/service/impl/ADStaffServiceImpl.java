package com.sd20201.datn.core.admin.staff.service.impl;

import com.sd20201.datn.core.admin.staff.model.request.ADCreateStaff;
import com.sd20201.datn.core.admin.staff.model.request.ADStaffRequest;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.admin.staff.service.ADStaffService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import com.sd20201.datn.infrastructure.email.EmailService;
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.utils.Helper;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ADStaffServiceImpl implements ADStaffService {

    private final ADStaffRepository repo;           // Repository của Staff
    private final AccountRepository accountRepository; // Repository của Account
    private final EmailService emailService;        // Service gửi mail

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
            default -> "NV";
        };
        return prefix + (new Random().nextInt(9000) + 1000);
    }

    @Override
    public ResponseObject<?> getAllStaff(ADStaffRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");
        RoleConstant roleEnum = null;
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try { roleEnum = RoleConstant.valueOf(request.getRole()); } catch (Exception e) {}
        }
        EntityStatus statusEnum = null;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try { statusEnum = EntityStatus.valueOf(request.getStatus()); } catch (Exception e) {}
        }
        return new ResponseObject<>(
                PageableObject.of(repo.getAllStaff(pageable, request.getName(), request.getKey(), statusEnum, roleEnum)),
                HttpStatus.OK, "Lấy thành công danh sách nhân viên"
        );
    }

    @Override
    @Transactional // Quan trọng để rollback nếu lỗi
    public ResponseObject<?> createStaff(ADCreateStaff request) {
        // --- 1. CHECK TRÙNG LẶP ---
        if (repo.findByPhone(request.getPhone()).isPresent()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại", false, "PHONE_EXISTS");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (repo.findByEmail(request.getEmail()).isPresent()) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Email đã tồn tại", false, "EMAIL_EXISTS");
            }
        }
        if (repo.findByCitizenIdentifyCard(request.getCitizenIdentifyCard()).isPresent()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Số CCCD/CMND đã tồn tại", false, "CCCD_EXISTS");
        }

        // --- 2. LOGIC TẠO TÀI KHOẢN & STAFF ---
        RoleConstant roleEnum = RoleConstant.valueOf(request.getRole());
        String username = generateUsername(request.getFullName());
        String password = generatePassword();
        String staffCode = generateStaffCode(roleEnum);

        // Tạo Account
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(password); // Nên mã hóa password nếu có security
        account.setRoleConstant(roleEnum);
        account = accountRepository.save(account);

        // Tạo Staff
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

        // Set mặc định trạng thái
        staff.setStatus(EntityStatus.ACTIVE);

        staff = repo.save(staff);

        // --- 3. GỬI EMAIL ---
        try {
            emailService.sendNewStaffEmail(request.getEmail(), request.getFullName(), staffCode, username, password);
        } catch (MessagingException e) {
            e.printStackTrace(); // Log lỗi gửi mail nhưng không chặn việc tạo user
        }

        return new ResponseObject<>(staff, HttpStatus.OK, "Thêm thành công nhân viên", true, null);
    }

    @Override
    @Transactional
    public ResponseObject<?> updateStaff(String id, ADCreateStaff request) {
        Staff staff = repo.findById(id).orElse(null);
        if (staff == null) {
            return new ResponseObject<>(null, HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên", false, null);
        }

        // --- 1. CHECK TRÙNG (Ngoại trừ chính mình) ---
        Optional<Staff> phoneCheck = repo.findByPhone(request.getPhone());
        if (phoneCheck.isPresent() && !phoneCheck.get().getId().equals(id)) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại", false, "PHONE_EXISTS");
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            Optional<Staff> emailCheck = repo.findByEmail(request.getEmail());
            if (emailCheck.isPresent() && !emailCheck.get().getId().equals(id)) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Email đã tồn tại", false, "EMAIL_EXISTS");
            }
        }

        Optional<Staff> cccdCheck = repo.findByCitizenIdentifyCard(request.getCitizenIdentifyCard());
        if (cccdCheck.isPresent() && !cccdCheck.get().getId().equals(id)) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Số CCCD/CMND đã tồn tại", false, "CCCD_EXISTS");
        }

        // --- 2. CẬP NHẬT ACCOUNT (Nếu đổi chức vụ) ---
        if (request.getRole() != null) {
            Account account = staff.getAccount();
            if (account != null) {
                account.setRoleConstant(RoleConstant.valueOf(request.getRole()));
                accountRepository.save(account);
            }
        }

        // --- 3. CẬP NHẬT STAFF ---
        staff.setName(request.getFullName());
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

        repo.save(staff);

        return new ResponseObject<>(staff, HttpStatus.OK, "Cập nhật nhân viên thành công", true, null);
    }

    @Override
    public ResponseObject<?> changeStatusStaff(String id) {
        Optional<Staff> optionallStaff = repo.findById(id);
        if (optionallStaff.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Nhân viên không tồn tại", false, null);
        }
        Staff staff = optionallStaff.get();
        staff.setStatus(staff.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        repo.save(staff);
        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái thành công", true, null);
    }

    @Override
    public ResponseObject<?> getStaffById(String id) {
        Optional<Staff> staffCheck = repo.findById(id);
        if (staffCheck.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên với ID: " + id);
        }
        return new ResponseObject<>(staffCheck.get(), HttpStatus.OK, "Lấy thông tin nhân viên thành công");
    }
}
