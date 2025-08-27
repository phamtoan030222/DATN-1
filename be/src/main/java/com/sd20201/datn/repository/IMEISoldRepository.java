package com.sd20201.datn.repository;

import com.sd20201.datn.entity.IMEISold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IMEISoldRepository extends JpaRepository<IMEISold, String> {
}
