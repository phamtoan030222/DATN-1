package com.sd20201.datn.core.client.invoice.model.response;

import java.math.BigDecimal;

public interface ClientInvoiceDetailsResponse {

    String getId();

    String getCode();

    BigDecimal getPrice();

    Integer getQuantity();

    BigDecimal getTotalAmount();

    String getNameProductDetail();

    String getUrlImage();

    String getColor();

    String getCpu();

    String getGpu();

    String getHardDrive();

    String getMaterial();

    String getProduct();

    String getRam();

    String getIdInvoice();
}
