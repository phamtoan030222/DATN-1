package com.sd20201.datn.core.admin.staff.service.impl;

import com.sd20201.datn.core.admin.staff.model.request.ADCreateStaff;
import com.sd20201.datn.core.admin.staff.model.request.ADStaffRequest;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.admin.staff.service.ADStaffService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADStaffServiceImpl implements ADStaffService {

    private final ADStaffRepository repo;

    @Override
    public ResponseObject<?> getAllStaff(ADStaffRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");

        // Convert String role sang RoleConstant
        RoleConstant roleEnum = null;
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                roleEnum = RoleConstant.valueOf(request.getRole());
            } catch (IllegalArgumentException e) {
                // Nếu String không hợp lệ, giữ roleEnum = null
                // Có thể log hoặc ném exception tuỳ nhu cầu
            }
        }
        EntityStatus statusEnum = null;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                statusEnum = EntityStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                // Log hoặc giữ null nếu giá trị không hợp lệ
            }
        }


        return new ResponseObject<>(
                PageableObject.of(repo.getAllStaff(
                        pageable,
                        request.getName(),
                        request.getKey(),
                        statusEnum,
                        roleEnum// Truyền enum thay vì String

                )),
                HttpStatus.OK,
                "Lấy thành công danh sách nhân viên"
        );
    }


    @Override
    public ResponseObject<?> createStaff(ADCreateStaff request) {
        return null;
    }

    @Override
    public ResponseObject<?> updateStaff(String id, ADCreateStaff request) {
        return null;
    }

    // Giả sử mình đã đổi tên biến repository thành adStaffRepository cho dễ đọc nhé
    @Override
    public ResponseObject<?> changeStatusStaff(String id) {
        Optional<Staff> optionallStaff = repo.findById(id); // Sửa tên biến repository

        if (optionallStaff.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Nhân viên không tồn tại", false, null);
        }

        Staff staff = optionallStaff.get();

        // --- SỬA LOGIC Ở ĐÂY ---
        // Cách 1: Dùng toán tử 3 ngôi
        staff.setStatus(staff.getStatus().equals(EntityStatus.ACTIVE)
                ? EntityStatus.INACTIVE
                : EntityStatus.ACTIVE);

        repo.save(staff);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái thành công", true, null);
    }


}
