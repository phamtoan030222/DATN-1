package com.sd20201.datn.core.admin.products.harddrive.repository;

import com.sd20201.datn.core.admin.products.harddrive.model.response.ADHardDriveResponse;
import com.sd20201.datn.entity.HardDrive;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.HardDriveRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ADHardDriveRepository extends HardDriveRepository {

    @Query(
            value = """
        SELECT hd.id          AS id,
               hd.brand       AS brand,
               hd.code        AS code,
               hd.type        AS type,
               hd.typeConnect AS typeConnect,
               hd.capacity    AS capacity,
               hd.readSpeed   AS readSpeed,
               hd.writeSpeed  AS writeSpeed,
               hd.cacheMemory AS cacheMemory,
               hd.physicalSize AS physicalSize,
               hd.description AS description,
               hd.status      AS status,
               hd.name        AS name
        FROM HardDrive hd
        WHERE
            (
                :key IS NULL
                OR hd.name        LIKE CONCAT('%', :key, '%')
                OR hd.code        LIKE CONCAT('%', :key, '%')
                OR hd.brand       LIKE CONCAT('%', :key, '%')
                OR hd.type        LIKE CONCAT('%', :key, '%')
            )
            AND (:status IS NULL OR hd.status = :status)
        ORDER BY hd.createdDate DESC
    """,
            countQuery = """
        SELECT COUNT(hd.id)
        FROM HardDrive hd
        WHERE
            (
                :key IS NULL
                OR hd.name        LIKE CONCAT('%', :key, '%')
                OR hd.code        LIKE CONCAT('%', :key, '%')
                OR hd.brand       LIKE CONCAT('%', :key, '%')
                OR hd.type        LIKE CONCAT('%', :key, '%')
            )
            AND (:status IS NULL OR hd.status = :status)
    """
    )
    Page<ADHardDriveResponse> getAllHardDrives(
            Pageable pageable,
            @Param("key") String key,
            @Param("status") EntityStatus status
    );

    /**
     * Lấy tất cả ổ cứng cùng hãng — dùng để check trùng trong Java
     */
    @Query("SELECT hd FROM HardDrive hd WHERE LOWER(TRIM(hd.brand)) = LOWER(TRIM(:brand))")
    List<HardDrive> findAllByBrand(@Param("brand") String brand);}
