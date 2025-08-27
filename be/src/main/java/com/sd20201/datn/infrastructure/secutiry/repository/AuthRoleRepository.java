package com.sd20201.datn.infrastructure.secutiry.repository;

import com.sd20201.datn.repository.RoleRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthRoleRepository extends RoleRepository {

    @Query(value = """
    SELECT r.code
    FROM Role r
    JOIN Account a ON r.id = a.role.id
    WHERE a.username = :username
    """)
    List<String> getRoleCodeByUsername(@Param("username") String username);

}
