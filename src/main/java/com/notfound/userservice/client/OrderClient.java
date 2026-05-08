package com.notfound.userservice.client;

import com.notfound.userservice.model.dto.response.UserStatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${app.client.order-service.url:http://order-service:8080}")
public interface OrderClient {

    @GetMapping("/api/v1/orders/stats")
    UserStatsResponse getOrderStats(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    );
}
