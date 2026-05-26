package com.notfound.userservice.model.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class SpringPageResponse<T> {
    List<T> content;
    long totalElements;
    int totalPages;
    int number;
    int size;
}
