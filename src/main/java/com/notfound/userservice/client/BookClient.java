package com.notfound.userservice.client;

import com.notfound.userservice.model.dto.request.BatchBookRequest;
import com.notfound.userservice.model.dto.response.BatchBookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "book-service", url = "${app.client.book-service.url:http://book-service:8080}")
public interface BookClient {

    @PostMapping("/api/v1/books/batch")
    BatchBookResponse getBooksBatch(@RequestBody BatchBookRequest request);

    @GetMapping("/api/v1/books/{bookId}")
    ResponseEntity<Object> getBookById(@PathVariable("bookId") UUID bookId);
}
