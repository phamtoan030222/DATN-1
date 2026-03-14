package com.sd20201.datn.repository;

import com.sd20201.datn.entity.WardShippingMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WardShippingMappingRepository extends JpaRepository<WardShippingMapping, Long> {
    Optional<WardShippingMapping> findByNewProvinceNameAndNewWardName(
            String newProvinceName,
            String newWardName
    );
}
