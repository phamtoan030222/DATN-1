package com.sd20201.datn.core.admin.shift.model.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class EndShiftRequest {
    private String accountId;     // ID người chốt ca
    private String shiftId;       // ID ca đang chốt
    private BigDecimal realCash;  // Tiền thực tế đếm được trong két (Quan trọng)
    private String note;
}
