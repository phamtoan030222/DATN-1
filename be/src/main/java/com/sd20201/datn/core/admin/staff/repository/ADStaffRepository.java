package com.sd20201.datn.core.admin.staff.repository;

import com.sd20201.datn.core.admin.staff.model.response.ADStaffResponse;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import com.sd20201.datn.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ADStaffRepository extends StaffRepository {

    @Query(
            value = """
    SELECT s.id AS id,
           s.code AS code,
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
    WHERE (:ten IS NULL OR s.name LIKE CONCAT('%', :ten, '%'))
      AND (:status IS NULL OR s.status = :status)
      AND (:role IS NULL OR s.account.roleConstant = :role)
      AND (:key IS NULL OR s.email LIKE CONCAT('%', :key, '%')
                               OR s.phone LIKE CONCAT('%', :key, '%'))
    ORDER BY s.lastModifiedDate DESC
    """,
            countQuery = """
    SELECT COUNT(s.id)
    FROM Staff s
    WHERE (:ten IS NULL OR s.name LIKE CONCAT('%', :ten, '%'))
      AND (:status IS NULL OR s.status = :status)
      AND (:role IS NULL OR s.account.roleConstant = :role)
      AND (:key IS NULL OR s.email LIKE CONCAT('%', :key, '%') 
                               OR s.phone LIKE CONCAT('%', :key, '%'))
    """
    )
    Page<ADStaffResponse> getAllStaff(
            Pageable pageable,
            @Param("ten") String ten,
            @Param("key") String key,
            @Param("status") EntityStatus status,
            @Param("role") RoleConstant role
    );

    // 1. Tìm theo SĐT
    Optional<Staff> findByPhone(String phone);

    // 2. Tìm theo Email
    Optional<Staff> findByEmail(String email);

    // 3. Tìm theo CCCD
    Optional<Staff> findByCitizenIdentifyCard(String citizenIdentifyCard);

    // của tâm thêm
    @Query("SELECT s.email FROM Staff s WHERE s.account.roleConstant = :role AND s.status = :status")
    List<String> findEmailsByRoleAndStatus(@Param("role") RoleConstant role, @Param("status") EntityStatus status);
}