package com.sd20201.datn.core.admin.statistics.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
public class AdDashboardOverviewResponse {
    private StatisticCard today;
    private StatisticCard week;
    private StatisticCard month;
    private StatisticCard year;
    private List<TopItemDTO> topSellingProducts;

    @Data @Builder
    public static class StatisticCard {
        private Long revenue;
        private Long expectedRevenue;
        private Integer soldProducts;
        private Integer totalOrders;
        private Integer completed;
        private Integer cancelled;
        private Integer processing;
        private String growthRate;
    }

    @Data @Builder
    public static class TopItemDTO {
        private String name;
        private Long count;
        private Double price;
        private String image;
    }
}
