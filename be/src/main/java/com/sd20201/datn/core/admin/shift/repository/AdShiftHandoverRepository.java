package com.sd20201.datn.core.admin.shift.repository;

import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.repository.ShiftHandoverRepository; // Import repo gốc
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdShiftHandoverRepository extends ShiftHandoverRepository {


    @Query("SELECT s FROM ShiftHandover s WHERE s.account.id = :accountId AND s.status = com.sd20201.datn.infrastructure.constant.EntityStatus.ACTIVE")
    Optional<ShiftHandover> findOpenShiftByAccountId(@Param("accountId") String accountId);
}