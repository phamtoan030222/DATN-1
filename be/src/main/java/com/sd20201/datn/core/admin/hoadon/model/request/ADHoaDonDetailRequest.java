package com.sd20201.datn.core.admin.hoadon.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ADHoaDonDetailRequest extends PageableRequest {

    private String maHoaDon;

}
