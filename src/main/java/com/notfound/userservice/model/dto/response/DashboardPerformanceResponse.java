package com.notfound.userservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPerformanceResponse {
    Metric conversionRate;
    Metric satisfactionRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metric {
        Double current;
        Double target;
    }
}
