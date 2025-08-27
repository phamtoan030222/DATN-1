package com.sd20201.datn.infrastructure.config.database.repository;

import com.sd20201.datn.entity.Account;
import com.sd20201.datn.repository.AccountRepository;

import java.util.Optional;

public interface DBAccountRepository extends AccountRepository {

    Optional<Account> findByUsername(String username);

}
