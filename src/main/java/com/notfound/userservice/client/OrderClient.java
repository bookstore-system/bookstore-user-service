package com.notfound.userservice.client;

import com.notfound.userservice.model.dto.response.UserStatsResponse;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.DashboardOrderResponse;
import com.notfound.userservice.model.dto.response.SpringPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${app.client.order-service.url:http://order-service:8080}")
public interface OrderClient {

    @GetMapping("/api/v1/orders/stats")
    UserStatsResponse getOrderStats(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    );

    @GetMapping("/api/v1/orders/top-spenders")
    java.util.List<UserStatsResponse.TopUserResponse> getTopSpenders(
            @RequestParam(value = "limit", defaultValue = "5", required = false) Integer limit
    );

    @GetMapping("/api/v1/orders/top-buyers")
    java.util.List<UserStatsResponse.TopUserResponse> getTopBuyers(
            @RequestParam(value = "limit", defaultValue = "5", required = false) Integer limit
    );

    @GetMapping("/api/v1/orders/users/{userId}/summary")
    com.notfound.userservice.model.dto.response.UserOrderSummaryResponse getUserOrderSummary(
            @org.springframework.web.bind.annotation.PathVariable("userId") java.util.UUID userId
    );

    @GetMapping("/api/v1/orders/admin/all")
    ApiResponse<SpringPageResponse<DashboardOrderResponse>> getAdminOrders(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    );
}
