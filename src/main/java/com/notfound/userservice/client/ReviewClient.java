package com.notfound.userservice.client;

import com.notfound.userservice.model.dto.response.UserReviewCountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "review-service", url = "${app.client.review-service.url:http://review-service:8080}")
public interface ReviewClient {

    @GetMapping("/api/v1/reviews/users/{userId}/count")
    UserReviewCountResponse getUserReviewCount(@PathVariable("userId") UUID userId);
}
