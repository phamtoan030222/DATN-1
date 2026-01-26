package com.sd20201.datn.core.auth.register.service.impl;

import com.sd20201.datn.core.auth.register.model.request.AuthRegisterRequest;
import com.sd20201.datn.core.auth.register.repository.AuthRegisterAccountRepository;
import com.sd20201.datn.core.auth.register.repository.AuthRegisterCustomerRepository;
import com.sd20201.datn.core.auth.register.service.RegisterService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final AuthRegisterAccountRepository accountRepository;

    private final AuthRegisterCustomerRepository customerRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponseObject<?> register(AuthRegisterRequest request) {

        Optional<Account> accountOptional = accountRepository.findByUsername(request.getUsername());
        if (accountOptional.isPresent()) return ResponseObject.errorForward("Account is already exist", HttpStatus.CONFLICT);

        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setRoleConstant(RoleConstant.KHACH_HANG);
        account.setPassword(passwordEncoder.encode(request.getPassword()));

        accountRepository.save(account);
        Customer customer = new Customer();

        customer.setName(request.getUsername());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhoneNumber());
        customer.setBirthday(request.getBirthday());
        customer.setAccount(account);

        customerRepository.save(customer);

        return ResponseObject.successForward(customer.getId(), "Register success");
    }
}
