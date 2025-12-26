package com.sd20201.datn.core.admin.customer.repository;

import com.sd20201.datn.core.admin.customer.model.response.CustomerResponse;
import com.sd20201.datn.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdCustomerRepository extends com.sd20201.datn.repository.CustomerRepository {
    @Query(
            value = """
                    SELECT c.id AS id,
                           c.name AS customerName,
                           c.phone AS customerPhone,
                           c.email AS customerEmail,
                           c.avatarUrl AS customerAvatar,
                           c.account.id AS customerIdAccount,
                           c.code AS customerCode,
                           c.description AS customerDescription,
                           c.gender AS customerGender,
                           CASE WHEN c.status = 0 THEN 1 ELSE 0 END AS customerStatus,
                           c.birthday AS customerBirthday,
                           c.createdBy AS customerCreatedBy,
                           c.createdDate AS customerCreatedDate,
                           c.lastModifiedBy AS customerModifiedBy,
                           c.lastModifiedDate AS customerModifiedDate
                    FROM Customer c
                    WHERE
                      (:keyword IS NULL OR (
                                          c.name LIKE CONCAT('%', :keyword, '%')
                                          OR c.phone LIKE CONCAT('%', :keyword, '%')
                                          OR c.code LIKE CONCAT('%', :keyword, '%')
                                            ))
                      AND (:customerStatus IS NULL
                           OR (:customerStatus = 1 AND c.status = 0)
                           OR (:customerStatus = 0 AND c.status = 1))
                      AND (:customerGender IS NULL OR c.gender = :customerGender)
                    ORDER BY c.createdDate DESC
                    """,
            countQuery = """
                    SELECT COUNT(c.id)
                    FROM Customer c
                    WHERE 
                     (:keyword IS NULL OR (
                                          c.name LIKE CONCAT('%', :keyword, '%')
                                          OR c.phone LIKE CONCAT('%', :keyword, '%')
                                          OR c.code LIKE CONCAT('%', :keyword, '%')
                                            ))
                      AND (:customerStatus IS NULL 
                           OR (:customerStatus = 1 AND c.status = 0) 
                           OR (:customerStatus = 0 AND c.status = 1))
                      AND (:customerGender IS NULL OR c.gender = :customerGender)
                    """
    )
    Page<CustomerResponse> getAllCustomers(Pageable pageable,
                                           @Param("keyword") String keyword, // Gộp vào 1 biến này
                                           @Param("customerStatus") Integer customerStatus,
                                           @Param("customerGender") Boolean customerGender);
    // =========================
    // 🎯 Query tìm kiếm chính xác
    // =========================

    @Query("SELECT c FROM Customer c WHERE c.name = :name")
    List<Customer> findByExactName(@Param("name") String name);

    @Query(value = "SELECT * FROM customer c WHERE c.name = :name", nativeQuery = true)
    List<Customer> findByExactNameNative(@Param("name") String name);

    @Query("SELECT c FROM Customer c WHERE c.phone = :phone")
    List<Customer> findByExactPhone(@Param("phone") String phone);

    // =========================
    // 🆕 Query cho tự động tạo mã khách hàng
    // =========================

    /**
     * Tìm tất cả mã khách hàng theo pattern để tạo mã tự động
     * Ví dụ: pattern = "longvv%" sẽ tìm tất cả mã bắt đầu bằng "longvv"
     */
    @Query("SELECT c.code FROM Customer c WHERE c.code LIKE :pattern AND c.code IS NOT NULL")
    List<String> findCustomerCodesByPattern(@Param("pattern") String pattern);

    /**
     * Kiểm tra mã khách hàng đã tồn tại chưa
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.code = :code")
    Long countByCode(@Param("code") String code);

    /**
     * Tìm mã khách hàng lớn nhất theo base pattern
     * Ví dụ: baseCode = "longvv" sẽ tìm mã lớn nhất như longvv001, longvv002...
     */
    @Query(value = """
            SELECT c.code FROM customer c 
            WHERE c.code REGEXP CONCAT('^', :baseCode, '[0-9]{3}$') 
            ORDER BY c.code DESC 
            LIMIT 1
            """, nativeQuery = true)
    String findLatestCodeByBase(@Param("baseCode") String baseCode);

    // =========================
    // 🔍 Query hỗ trợ / tìm gần đúng
    // =========================

    @Query("SELECT c FROM Customer c WHERE UPPER(c.name) LIKE UPPER(CONCAT('%', :name, '%'))")
    List<Customer> findAllNameContaining(@Param("name") String name);

    @Query("""
            SELECT c.id AS id,
                   c.name AS customerName,
                   c.phone AS customerPhone,
                   c.email AS customerEmail,
                   c.avatarUrl AS customerAvatar,
                   c.account.id AS customerIdAccount,
                   c.code AS customerCode,
                   c.description AS customerDescription,
                   c.gender AS customerGender,
                   CASE WHEN c.status = 0 THEN 1 ELSE 0 END AS customerStatus,
                   c.birthday AS customerBirthday,
                   c.createdBy AS customerCreatedBy,
                   c.createdDate AS customerCreatedDate,
                   c.lastModifiedBy AS customerModifiedBy,
                   c.lastModifiedDate AS customerModifiedDate
            FROM Customer c
            WHERE c.id = :id
            """)
    CustomerResponse findCustomerById(@Param("id") String id);

    List<Customer> findByEmail(String email);


}
