package com.sd20201.datn.core.admin.products.cpu.model.response;

import com.sd20201.datn.core.common.base.IsEntityStatus;
import com.sd20201.datn.core.common.base.IsIdentify;

public interface ADProductCPUResponse extends IsIdentify, IsEntityStatus {

    String getCode();

    String getName();

    String getDescription();

    String getGeneration();

    String getSeries();

    String getBrand();

    Integer getReleaseYear();

    Long getCreatedDate();

}
