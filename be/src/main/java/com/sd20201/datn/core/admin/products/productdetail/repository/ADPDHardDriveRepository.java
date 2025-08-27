package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.repository.HardDriveRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDHardDriveRepository extends HardDriveRepository {

    @Query(value = """ 
    SELECT h.id as value, h.name as label FROM HardDrive h
    """)
    List<ADPDPropertyComboboxResponse> getHardDrives();

}
