package com.sd20201.datn.core.admin.statistics.model.response;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder

public class AdRevenueChartResponse {
    private List<String> timeLabels;
    private List<Long> currentData;
    private List<Long> previousData;
}
