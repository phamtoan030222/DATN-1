package com.sd20201.datn.repository;

import com.sd20201.datn.entity.IpnLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IpnLogRepository extends JpaRepository<IpnLog, Long> {
}