package com.sd20201.datn.core.client.banhang.model.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClientVoucherSuggestionResponse {

    private List<ClientApplicableVoucherResponse> voucherApDung;
    private List<ClientBetterVoucherResponse> voucherTotHon;
}
