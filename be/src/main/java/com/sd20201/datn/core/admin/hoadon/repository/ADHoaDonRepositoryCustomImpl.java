package com.sd20201.datn.core.admin.hoadon.repository;

import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonSearchRequest;
import com.sd20201.datn.core.admin.hoadon.model.response.ADHoaDonResponse;
import com.sd20201.datn.core.admin.hoadon.model.response.HoaDonPageResponse;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ADHoaDonRepositoryCustomImpl
        implements ADHoaDonRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    public HoaDonPageResponse getAllHoaDonResponse(ADHoaDonSearchRequest request, Pageable pageable) {
        String hql = """
        SELECT new com.sd20201.datn.core.admin.hoadon.model.response.ADHoaDonResponse(
                        hd.id,
                        hd.code,
                        kh.name,
                        kh.phone,
                        nv.code,
                        nv.name,
                        hd.totalAmountAfterDecrease,
                        hd.typeInvoice,
                        hd.createdDate,
                        hd.entityTrangThaiHoaDon
                    )
                    FROM Invoice hd
                    LEFT JOIN hd.customer kh
                    LEFT JOIN hd.staff nv
                    WHERE (:q IS NULL OR :q = '' 
                        OR LOWER(kh.name) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(hd.code) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(hd.name) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(kh.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(nv.code) LIKE LOWER(CONCAT('%', :q, '%')))
                      AND (:trangThai IS NULL OR hd.entityTrangThaiHoaDon = :trangThai)
                      AND (:startDate IS NULL OR CAST(hd.createdDate AS BIGINTEGER) >= :startDate)
                      AND (:endDate IS NULL OR CAST(hd.createdDate AS BIGINTEGER) <= :endDate)
                      AND hd.entityTrangThaiHoaDon != :luuTam
                    ORDER BY hd.createdDate ASC
                """;

        String countByStatusHql = """
                    SELECT hd.entityTrangThaiHoaDon, COUNT(hd)
                    FROM Invoice hd
                    LEFT JOIN hd.customer kh
                    LEFT JOIN hd.staff nv
                    WHERE (:q IS NULL OR :q = '' 
                        OR LOWER(kh.name) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(kh.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(nv.name) LIKE LOWER(CONCAT('%', :q, '%')))
                      AND (:startDate IS NULL OR CAST(hd.createdDate AS BIGINTEGER) >= :startDate)
                      AND (:endDate IS NULL OR CAST(hd.createdDate AS BIGINTEGER) <= :endDate)
                      AND hd.entityTrangThaiHoaDon != :luuTam
                    GROUP BY hd.entityTrangThaiHoaDon
                """;

        String totalCountHql = """
                    SELECT COUNT(hd)
                    FROM Invoice hd
                    LEFT JOIN hd.customer kh
                    LEFT JOIN hd.staff nv
                    WHERE (:q IS NULL OR :q = '' 
                        OR LOWER(kh.name) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(kh.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(nv.name) LIKE LOWER(CONCAT('%', :q, '%')))
                      AND (:trangThai IS NULL OR hd.entityTrangThaiHoaDon = :trangThai)
                      AND (:startDate IS NULL OR CAST(hd.createdDate AS BIGINTEGER) >= :startDate)
                      AND (:endDate IS NULL OR CAST(hd.createdDate AS BIGINTEGER) <= :endDate)
                      AND hd.entityTrangThaiHoaDon != :luuTam
                """;

        List<Object[]> countByStatusList = entityManager.createQuery(countByStatusHql)
                .setParameter("q", request.getQ() == null ? "" : request.getQ().trim())
                .setParameter("startDate", request.getStartDate())
                .setParameter("endDate", request.getEndDate())
                .setParameter("luuTam", EntityTrangThaiHoaDon.LUU_TAM)
                .getResultList();

        Map<EntityTrangThaiHoaDon, Long> countByStatusMap = new HashMap<>();
        for (Object[] row : countByStatusList) {
            EntityTrangThaiHoaDon status = (EntityTrangThaiHoaDon) row[0];
            Long count = (Long) row[1];

            // Bỏ qua nếu là trạng thái LƯU_TẠM
            if (status == EntityTrangThaiHoaDon.LUU_TAM) {
                continue;
            }

            countByStatusMap.put(status, count);
        }


        Long totalRecords = (Long) entityManager.createQuery(totalCountHql)
                .setParameter("q", request.getQ() == null ? "" : request.getQ().trim())
                .setParameter("trangThai", request.getStatus())
                .setParameter("startDate", request.getStartDate())
                .setParameter("endDate", request.getEndDate())
                .setParameter("luuTam", EntityTrangThaiHoaDon.LUU_TAM)
                .getSingleResult();

        List<ADHoaDonResponse> hoaDonResponses = entityManager.createQuery(hql, ADHoaDonResponse.class)
                .setParameter("q", request.getQ() == null ? "" : request.getQ().trim())
                .setParameter("trangThai", request.getStatus())
                .setParameter("startDate", request.getStartDate())
                .setParameter("endDate", request.getEndDate())
                .setParameter("luuTam", EntityTrangThaiHoaDon.LUU_TAM)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Page<ADHoaDonResponse> pageResult = new PageImpl<>(hoaDonResponses, pageable, totalRecords);
        return new HoaDonPageResponse(pageResult, countByStatusMap);
    }


}
