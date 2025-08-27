package com.sd20201.datn.infrastructure.secutiry.repository;

import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.repository.StaffRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthStaffRepository extends StaffRepository {

    @Query(value = """
    SELECT s
        from Staff s JOIN Account a on s.account.id = a.id
        WHERE a.username = :username
    """)
    Optional<Staff> findByUsername(@Param("username") String username);

    Optional<Staff> findByEmail(String email);

}
