package com.sd20201.datn.core.admin.staff.repository;

import com.sd20201.datn.core.admin.staff.model.response.ADStaffResponse;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import com.sd20201.datn.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ADStaffRepository extends StaffRepository {

    @Query(
            value = """
    SELECT s.id AS id,
           s.name AS fullName,
           s.account.roleConstant AS role,
           s.gender AS gender,
           s.email AS email,
           s.phone AS phone,
           s.status AS status,
           s.hometown AS hometown,
           s.avatarUrl AS avatarUrl,
           s.birthday AS birthday,
           s.citizenIdentifyCard AS citizenIdentifyCard,
           s.provinceCode AS provinceCode,
           s.districtCode AS districtCode,
           s.communeCode AS communeCode
    FROM Staff s
    WHERE (:fullName IS NULL OR s.name LIKE CONCAT('%', :fullName, '%'))
      AND (:role IS NULL OR s.account.roleConstant = :role)
      AND (:email IS NULL OR s.email LIKE CONCAT('%', :email, '%'))
      AND (:phone IS NULL OR s.phone LIKE CONCAT('%', :phone, '%'))
      AND (:status IS NULL OR s.status = :status)
    ORDER BY s.createdBy DESC
    """,
            countQuery = """
    SELECT COUNT(s.id)
    FROM Staff s
    WHERE (:fullName IS NULL OR s.name LIKE CONCAT('%', :fullName, '%'))
      AND (:role IS NULL OR s.account.roleConstant = :role)
      AND (:email IS NULL OR s.email LIKE CONCAT('%', :email, '%'))
      AND (:phone IS NULL OR s.phone LIKE CONCAT('%', :phone, '%'))
      AND (:status IS NULL OR s.status = :status)
    """
    )
    Page<ADStaffResponse> getAllStaff(
            Pageable pageable,
            @Param("fullName") String fullName,
            @Param("role") RoleConstant role, // <-- quan trọng: đổi từ String sang RoleConstant
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("status") EntityStatus status
    );


}
