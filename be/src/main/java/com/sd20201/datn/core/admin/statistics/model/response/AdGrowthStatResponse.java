package com.sd20201.datn.core.admin.statistics.model.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdGrowthStatResponse {
    private String label;
    private String value;
    private Double percent;
    @JsonProperty("isIncrease")
    private boolean isIncrease;
    private String trend;
    private String type;
}
