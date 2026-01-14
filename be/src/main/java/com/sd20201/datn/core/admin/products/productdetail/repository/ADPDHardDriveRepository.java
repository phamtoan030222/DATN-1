package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.entity.HardDrive;
import com.sd20201.datn.repository.HardDriveRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADPDHardDriveRepository extends HardDriveRepository {

    @Query(value = """ 
    SELECT h.id as value, h.name as label FROM HardDrive h
    ORDER BY h.createdDate desc
    """)
    List<ADPDPropertyComboboxResponse> getHardDrives();

    @Query(value = """ 
    SELECT h FROM HardDrive h
    WHERE LOWER(h.name) = LOWER(:name)
    """)
    Optional<HardDrive> findByName(String name);
}
