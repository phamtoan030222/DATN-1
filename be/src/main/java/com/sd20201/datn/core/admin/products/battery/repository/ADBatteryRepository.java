package com.sd20201.datn.core.admin.products.battery.repository;

import com.sd20201.datn.core.admin.products.battery.model.response.ADBatteryResponse;
import com.sd20201.datn.repository.BatteryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ADBatteryRepository extends BatteryRepository {
    @Query(
            value = """
        SELECT b.id AS id,
               b.code AS code,
               b.name AS name,
               b.brand AS brand,
               b.technolyCharging AS technolyCharging,
               b.capacity AS capacity,
               b.removeBattery AS removeBattery,
               b.typeBattery AS type,
               b.status AS status
        FROM Battery b
        WHERE (:key IS NULL 
               OR b.name LIKE CONCAT('%', :key, '%') 
               OR b.brand LIKE CONCAT('%', :key, '%'))
        ORDER BY b.createdDate DESC
    """,
            countQuery = """
        SELECT COUNT(b.id)
        FROM Battery b
        WHERE (:key IS NULL 
               OR b.name LIKE CONCAT('%', :key, '%') 
               OR b.brand LIKE CONCAT('%', :key, '%'))
    """
    )
    Page<ADBatteryResponse> getAllBatteries(
            Pageable pageable,
            @Param("key") String key
    );
}
