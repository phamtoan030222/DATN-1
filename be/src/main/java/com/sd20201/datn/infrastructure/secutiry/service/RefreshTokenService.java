package com.sd20201.datn.infrastructure.secutiry.service;

import com.sd20201.datn.entity.RefreshToken;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.infrastructure.secutiry.repository.AuthRefreshTokenRepository;
import com.sd20201.datn.infrastructure.secutiry.repository.AuthStaffRepository;
import com.sd20201.datn.infrastructure.secutiry.user.UserPrincipal;
import com.sd20201.datn.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final long REFRESH_EXPIRED_TIME = 6 * 60 * 60 * 1000;

    private final AuthRefreshTokenRepository authRefreshTokenRepository;

    private final AuthStaffRepository staffRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return authRefreshTokenRepository.findByRefreshToken(token);
    }

    public RefreshToken createRefreshToken(Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        log.info("Creating new refresh token for user {}", principal.toString());

        Optional<Staff> staff = staffRepository.findByEmail(principal.getUsername());
        Optional<RefreshToken> optionalRefreshToken = authRefreshTokenRepository.findByUserId(principal.getId());

        if (optionalRefreshToken.isPresent()) {
            RefreshToken refreshToken = optionalRefreshToken.get();
            if (optionalRefreshToken.get().getRevokedAt() != null) {
                refreshToken.setRevokedAt(null);
                refreshToken.setExpiredAt(REFRESH_EXPIRED_TIME);
                refreshToken.setRefreshToken(UUID.randomUUID().toString());
                return authRefreshTokenRepository.save(refreshToken);
            }
            refreshToken.setExpiredAt(REFRESH_EXPIRED_TIME);
            refreshToken.setRefreshToken(UUID.randomUUID().toString());
            return authRefreshTokenRepository.save(refreshToken);
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(principal.getId());
        refreshToken.setExpiredAt(REFRESH_EXPIRED_TIME);
        refreshToken.setRefreshToken(UUID.randomUUID().toString());
        return authRefreshTokenRepository.save(refreshToken);
    }

    public RefreshToken createRefreshToken(String userID) {
        Optional<RefreshToken> optionalRefreshToken = authRefreshTokenRepository.findByUserId(userID);

        if (optionalRefreshToken.isPresent()) {
            RefreshToken refreshToken = optionalRefreshToken.get();
            if (optionalRefreshToken.get().getRevokedAt() != null) {
                refreshToken.setRevokedAt(null);
                refreshToken.setExpiredAt(REFRESH_EXPIRED_TIME);
                refreshToken.setRefreshToken(UUID.randomUUID().toString());
                return authRefreshTokenRepository.save(refreshToken);
            }
            refreshToken.setExpiredAt(REFRESH_EXPIRED_TIME);
            refreshToken.setRefreshToken(UUID.randomUUID().toString());
            return authRefreshTokenRepository.save(refreshToken);
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userID);
        refreshToken.setExpiredAt(REFRESH_EXPIRED_TIME);
        refreshToken.setRefreshToken(UUID.randomUUID().toString());
        return authRefreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken refreshToken) {
        if(refreshToken.getExpiredAt() < DateTimeUtil.convertDateToTimeStampSecond(new Date())) {
            authRefreshTokenRepository.delete(refreshToken);
            return null;
        }

        return refreshToken ;
    }
}
