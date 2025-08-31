package com.sd20201.datn.core.admin.staff.service.impl;

import com.sd20201.datn.core.admin.staff.model.request.ADCreateStaff;
import com.sd20201.datn.core.admin.staff.model.request.ADStaffRequest;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.admin.staff.service.ADStaffService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ADStaffServiceImpl implements ADStaffService {

    private final ADStaffRepository adStaffService;

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
                PageableObject.of(adStaffService.getAllStaff(
                        pageable,
                        request.getName(),
                        roleEnum,       // Truyền enum thay vì String
                        request.getEmail(),
                        request.getPhone(),
                        statusEnum

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
}
