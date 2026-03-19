package com.sd20201.datn.core.client.invoice.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientPutReceiverRequest {

    private String tenKhachHang;

    private String sdtKH;

    private String email;

    private String diaChi;
}
