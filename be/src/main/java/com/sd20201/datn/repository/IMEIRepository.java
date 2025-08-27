package com.sd20201.datn.repository;

import com.sd20201.datn.entity.IMEI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IMEIRepository extends JpaRepository<IMEI, String> {
}
