package com.sd20201.datn.core.admin.statistics.repository;

import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdCustomerRepository1 extends JpaRepository<Customer, String> {
    // Đếm tổng khách hàng
    @Query(value = "SELECT COUNT(*) FROM customer WHERE status = 0 ", nativeQuery = true)
    Integer countAllCustomers();

    // Đếm khách hàng mới đăng ký trong khoảng thời gian
    @Query(value = "SELECT COUNT(*) FROM customer WHERE created_date BETWEEN :start AND :end", nativeQuery = true)
    Integer countNewCustomers(@Param("start") Long start, @Param("end") Long end);

    @Query(value = "SELECT created_date FROM customer ORDER BY created_date DESC", nativeQuery = true)
    List<Long> getAllCustomerCreatedDates();
}
