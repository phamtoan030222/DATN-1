package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDImeiResponse;
import com.sd20201.datn.entity.IMEI;
import com.sd20201.datn.repository.IMEIRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientPDImeiRepository extends IMEIRepository {

    Optional<IMEI> findByCode(String code);

    @Query(value = """
    SELECT i.code FROM IMEI i where i.code in :codes
    ORDER BY i.createdDate desc
    """)
    List<String> findByCode(List<String> codes);

    @Query(value = """
    SELECT i.id as id, i.code as code, i.name as name, i.status as status, i.imeiStatus as imeiStatus from IMEI i where i.productDetail.id = :idProductDetail AND i.imeiStatus = 0
    """)
    List<ClientPDImeiResponse> findByIdProductDetail(String idProductDetail);

}
