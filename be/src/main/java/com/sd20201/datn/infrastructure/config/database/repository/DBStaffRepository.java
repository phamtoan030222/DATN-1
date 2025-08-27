package com.sd20201.datn.infrastructure.config.database.repository;

import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.repository.StaffRepository;

import java.util.Optional;

public interface DBStaffRepository extends StaffRepository {

    Optional<Staff> findByEmail(String email);

}
