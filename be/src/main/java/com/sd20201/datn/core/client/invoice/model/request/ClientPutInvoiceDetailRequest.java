package com.sd20201.datn.core.client.invoice.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientPutInvoiceDetailRequest {

    private List<UpdateInvoiceDetail> updateProductDetails;

    private BigDecimal totalAmount;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateInvoiceDetail {
        private String idProductDetail;

        private String idInvoiceDetail;

        private Integer quantity;

        private BigDecimal price;

        private BigDecimal giaGoc;

        private BigDecimal totalAmount;
    }

}
