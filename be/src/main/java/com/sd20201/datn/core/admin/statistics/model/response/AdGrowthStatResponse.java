package com.sd20201.datn.core.admin.statistics.model.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdGrowthStatResponse {
    private String label;
    private String value;
    private String percent;
    private String trend;
}
