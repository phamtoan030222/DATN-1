package com.sd20201.datn.core.client.invoice.model.request;

import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientGetInvoicesRequest {

    private String searchQuery;

    private EntityTrangThaiHoaDon searchStatus;

    private TypeInvoice loaiHoaDon;

    private Long startDate;

    private Long endDate;

}
