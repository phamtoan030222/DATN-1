package com.sd20201.datn.repository;

import com.sd20201.datn.entity.GPU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GPURepository extends JpaRepository<GPU, String> {
}
