package com.notfound.userservice.client;

import com.notfound.userservice.model.dto.request.BatchBookRequest;
import com.notfound.userservice.model.dto.response.BatchBookResponse;
import com.notfound.userservice.model.dto.response.BookServiceApiResponse;
import com.notfound.userservice.model.dto.response.DashboardBookResponse;
import com.notfound.userservice.model.dto.response.SpringPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "book-service", url = "${app.client.book-service.url:http://book-service:8080}")
public interface BookClient {

    @PostMapping("/api/v1/books/batch")
    BookServiceApiResponse<BatchBookResponse> getBooksBatch(@RequestBody BatchBookRequest request);

    @GetMapping("/api/v1/books/{bookId}")
    ResponseEntity<Object> getBookById(@PathVariable("bookId") UUID bookId);

    @GetMapping("/api/v1/admin/books")
    BookServiceApiResponse<SpringPageResponse<DashboardBookResponse>> getAdminBooks(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "1000") int size
    );

    @GetMapping("/api/v1/books/best-selling")
    BookServiceApiResponse<List<DashboardBookResponse>> getBestSellingBooks(
            @RequestParam(value = "limit", defaultValue = "5") int limit
    );
}
