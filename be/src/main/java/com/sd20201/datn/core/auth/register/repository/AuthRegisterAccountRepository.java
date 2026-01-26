package com.sd20201.datn.core.auth.register.repository;

import com.sd20201.datn.entity.Account;
import com.sd20201.datn.repository.AccountRepository;

import java.util.Optional;

public interface AuthRegisterAccountRepository extends AccountRepository {

    Optional<Account> findByUsername(String username);

}
