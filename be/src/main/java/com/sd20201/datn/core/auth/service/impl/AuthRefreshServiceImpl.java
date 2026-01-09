package com.sd20201.datn.core.auth.service.impl;

import com.sd20201.datn.core.auth.model.request.AuthRefreshRequest;
import com.sd20201.datn.core.auth.model.response.AuthRefreshResponse;
import com.sd20201.datn.core.auth.repository.AuthRefreshCustomerRepository;
import com.sd20201.datn.core.auth.repository.AuthRefreshStaffRepository;
import com.sd20201.datn.core.auth.service.AuthRefreshService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.RefreshToken;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.secutiry.service.CustomUserDetailsService;
import com.sd20201.datn.infrastructure.secutiry.service.RefreshTokenService;
import com.sd20201.datn.infrastructure.secutiry.service.TokenProvider;
import com.sd20201.datn.infrastructure.secutiry.user.UserPrincipal;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthRefreshServiceImpl implements AuthRefreshService {

    private final RefreshTokenService refreshTokenService;

    private final TokenProvider tokenProvider;

    private final CustomUserDetailsService customUserDetailsService;

    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;

    @Override
    public ResponseObject<?> refresh(AuthRefreshRequest request) {
        log.info("Start refresh token");
        Optional<RefreshToken> refreshTokenOptional = refreshTokenService.findByToken(request.getRefreshToken());

        if (refreshTokenOptional.isEmpty())
            return ResponseObject.errorForward("Refresh is incorrect", HttpStatus.NOT_ACCEPTABLE);

        if (refreshTokenService.verifyExpiration(refreshTokenOptional.get()) == null)
            ResponseObject.errorForward("Refresh is expired", HttpStatus.NOT_ACCEPTABLE);

        return creatState(refreshTokenOptional.get())
                .map(token -> ResponseObject.successForward(token, "Refresh token success"))
                .orElse(ResponseObject.errorForward("Refresh token not found", HttpStatus.NOT_ACCEPTABLE));
    }

    private Optional<AuthRefreshResponse> creatState(RefreshToken refreshTokenEntity) {
        try {
            String username = null;
            Optional<Staff> staffOptional = staffRepository.findById(refreshTokenEntity.getUserId());
            if (staffOptional.isPresent()) username = staffOptional.get().getAccount().getUsername();

            Optional<Customer> customerOptional = customerRepository.findById(refreshTokenEntity.getUserId());
            if (customerOptional.isPresent()) username = customerOptional.get().getAccount().getUsername();

            if (username == null || username.isEmpty()) throw new RuntimeException("User id not found");
            UserPrincipal userPrincipal = (UserPrincipal) customUserDetailsService.loadUserByUsername(username);

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            userPrincipal.getAuthorities()
                    );

            String accessToken = tokenProvider.createTokenByLogin(authentication);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(refreshTokenEntity.getUserId());

            log.info("Refresh token created");

            return Optional.of(AuthRefreshResponse.of(accessToken, refreshToken.getRefreshToken()));
        } catch (Exception e) {
            log.error("Failure creating refresh token", e);
            log.error(e.getMessage());
            return Optional.empty();
        }
    }
}
