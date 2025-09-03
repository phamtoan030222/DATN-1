package com.sd20201.datn.core.admin.customer.repository;

import com.sd20201.datn.core.admin.customer.model.response.CustomerResponse;
import com.sd20201.datn.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
                WHERE (:customerName IS NULL OR c.name LIKE CONCAT('%', :customerName, '%'))
                  AND (:customerStatus IS NULL 
                       OR (:customerStatus = 1 AND c.status = 0) 
                       OR (:customerStatus = 0 AND c.status = 1))
                  AND (:customerGender IS NULL OR c.gender = :customerGender)
                ORDER BY c.createdDate DESC
                """,
            countQuery = """
                SELECT COUNT(c.id)
                FROM Customer c
                WHERE (:customerName IS NULL OR c.name LIKE CONCAT('%', :customerName, '%'))
                  AND (:customerStatus IS NULL 
                       OR (:customerStatus = 1 AND c.status = 0) 
                       OR (:customerStatus = 0 AND c.status = 1))
                  AND (:customerGender IS NULL OR c.gender = :customerGender)
                """
    )
    Page<CustomerResponse> getAllCustomers(Pageable pageable,
                                           @Param("customerName") String customerName,
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
    // 🔍 Query hỗ trợ / tìm gần đúng
    // =========================

    @Query("SELECT c FROM Customer c WHERE UPPER(c.name) LIKE UPPER(CONCAT('%', :name, '%'))")
    List<Customer> findAllNameContaining(@Param("name") String name);

}
