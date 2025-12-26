package com.sd20201.datn.infrastructure.job.common.model.response;

import com.sd20201.datn.core.common.base.IsIdentify;

public interface ExImportLogResponse extends IsIdentify {

    String getFileName();

    Integer getTotalSuccess();

    Integer getTotalError();

    Long getCreatedAt();

}
