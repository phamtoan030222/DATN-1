package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDPropertyComboboxResponse;
import com.sd20201.datn.entity.HardDrive;
import com.sd20201.datn.repository.HardDriveRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientPDHardDriveRepository extends HardDriveRepository {

    @Query(value = """ 
    SELECT h.id as value, h.name as label FROM HardDrive h
    ORDER BY h.createdDate desc
    """)
    List<ClientPDPropertyComboboxResponse> getHardDrives();

    @Query(value = """ 
    SELECT h FROM HardDrive h
    WHERE LOWER(h.name) = LOWER(:name)
    """)
    Optional<HardDrive> findByName(String name);
}
