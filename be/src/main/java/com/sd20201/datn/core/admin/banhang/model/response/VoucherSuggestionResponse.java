package com.sd20201.datn.core.admin.banhang.model.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VoucherSuggestionResponse {

    private List<ApplicableVoucherResponse> voucherApDung;
    private List<BetterVoucherResponse> voucherTotHon;
}
