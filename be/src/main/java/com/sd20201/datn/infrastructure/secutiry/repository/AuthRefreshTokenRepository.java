package com.sd20201.datn.infrastructure.secutiry.repository;


import com.sd20201.datn.entity.RefreshToken;
import com.sd20201.datn.repository.RefreshTokenAuthRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRefreshTokenRepository extends RefreshTokenAuthRepository {

    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    Optional<RefreshToken> findByUserId(String userId);
}
