package com.sd20201.datn.infrastructure.job.imei.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class IMEIExcelResponse {

    private String imei;

    private Boolean isExist;

}
