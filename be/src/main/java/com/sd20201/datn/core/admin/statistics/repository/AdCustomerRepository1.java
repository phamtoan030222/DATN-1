package com.sd20201.datn.core.admin.statistics.repository;

import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdCustomerRepository1 extends JpaRepository<Customer, String> {
    // 1. Đếm tổng khách hàng (Dựa theo Role, ví dụ Role KH là 2 hoặc tên là 'USER')
    // Bạn sửa lại điều kiện WHERE cho đúng Role khách hàng trong DB của bạn
    @Query(value = "SELECT COUNT(*) FROM customer", nativeQuery = true)
    Integer countAllCustomers();

    // 2. Đếm khách hàng mới đăng ký trong khoảng thời gian
    // created_date nằm trong class cha PrimaryEntity
    @Query(value = "SELECT COUNT(*) FROM customer WHERE created_date BETWEEN :start AND :end", nativeQuery = true)
    Integer countNewCustomers(@Param("start") Long start, @Param("end") Long end);
}
