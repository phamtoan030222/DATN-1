package com.sd20201.datn.core.client.invoice.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientInvoiceCancelRequest {

    private String id;

    private String note;

}
