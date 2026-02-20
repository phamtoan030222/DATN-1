package com.sd20201.datn.core.admin.shift.model.request;
import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShiftHistoryRequest extends PageableRequest{
    private String startDate; // Format: yyyy-MM-dd
    private String endDate;   // Format: yyyy-MM-dd
}
