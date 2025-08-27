package com.sd20201.datn.infrastructure.config.database.repository;


import com.sd20201.datn.entity.Role;
import com.sd20201.datn.repository.RoleRepository;

public interface DBRoleRepository extends RoleRepository {

    Role findByCode(String code);

}
