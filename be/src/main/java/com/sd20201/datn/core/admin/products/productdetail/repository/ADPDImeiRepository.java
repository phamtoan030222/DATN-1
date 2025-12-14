package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.entity.IMEI;
import com.sd20201.datn.repository.IMEIRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ADPDImeiRepository extends IMEIRepository {

    Optional<IMEI> findByCode(String code);

    @Query(value = """
    SELECT i.code FROM IMEI i where i.code in :codes
    """)
    List<String> findByCode(List<String> codes);
}
