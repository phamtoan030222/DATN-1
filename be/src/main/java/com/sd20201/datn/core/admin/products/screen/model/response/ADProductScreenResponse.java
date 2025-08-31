package com.sd20201.datn.core.admin.products.screen.model.response;

import com.sd20201.datn.core.common.base.IsEntityStatus;
import com.sd20201.datn.core.common.base.IsIdentify;
import com.sd20201.datn.infrastructure.constant.TypeScreenResolution;

public interface ADProductScreenResponse extends IsIdentify, IsEntityStatus {

    String getCode();

    String getName();

    Float getPhysicalSize();

    TypeScreenResolution getResolution();

    String getPanelType();

    String getTechnology();


}
