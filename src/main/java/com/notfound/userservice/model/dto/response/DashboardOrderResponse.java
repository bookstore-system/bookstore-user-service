package com.notfound.userservice.model.dto.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class DashboardOrderResponse {
    String id;
    String customerId;
    String status;
    BigDecimal total;
    String orderDate;
    String paymentMethod;
}
