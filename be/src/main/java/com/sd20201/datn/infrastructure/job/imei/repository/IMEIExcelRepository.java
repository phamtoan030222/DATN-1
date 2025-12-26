package com.sd20201.datn.infrastructure.job.imei.repository;

import com.sd20201.datn.entity.IMEI;
import com.sd20201.datn.repository.IMEIRepository;

import java.util.List;

public interface IMEIExcelRepository extends IMEIRepository {

    List<IMEI> findByNameIn(List<String> names);

}
