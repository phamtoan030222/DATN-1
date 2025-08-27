package com.sd20201.datn.repository;

import com.sd20201.datn.entity.Port;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortRepository extends JpaRepository<Port, String> {
}
