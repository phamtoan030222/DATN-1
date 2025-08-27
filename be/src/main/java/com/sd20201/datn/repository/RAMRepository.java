package com.sd20201.datn.repository;

import com.sd20201.datn.entity.Product;
import com.sd20201.datn.entity.RAM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RAMRepository extends JpaRepository<RAM, String> {
}
