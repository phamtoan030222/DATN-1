package com.sd20201.datn.core.admin.products.gpu.model.response;

import com.sd20201.datn.core.common.base.IsIdentify;

import java.io.Serializable;

public interface ADProductGPUResponse extends IsIdentify {

    String getCode();

    String getName();

    String getDescription();

    String getGeneration();

    String getSeries();

    String getBrand();

    Integer getReleaseYear();

}
