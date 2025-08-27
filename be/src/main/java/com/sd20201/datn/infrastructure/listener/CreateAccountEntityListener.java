package com.sd20201.datn.infrastructure.listener;


import com.sd20201.datn.entity.Account;
import jakarta.persistence.PrePersist;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CreateAccountEntityListener {

    private PasswordEncoder passwordEncoder;

    @PrePersist
    public void prePersist(Account account) {
        account.setPassword(passwordEncoder.encode(account.getPassword()));
    }
}
