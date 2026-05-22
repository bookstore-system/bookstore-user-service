package com.notfound.userservice.controller;

import com.notfound.userservice.client.OrderClient;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.DashboardOrderResponse;
import com.notfound.userservice.model.dto.response.RevenueStatisticResponse;
import com.notfound.userservice.model.dto.response.SpringPageResponse;
import com.notfound.userservice.model.dto.response.UserStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminStatisticsController {

    private static final int ORDER_PAGE_SIZE = 100;
    private static final int MAX_ORDER_PAGES = 10;

    private final OrderClient orderClient;

    @GetMapping("/revenue")
    public ApiResponse<RevenueStatisticResponse> getRevenueStatistics(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = parseDate(startDate, LocalDate.now().minusMonths(5).withDayOfMonth(1));
        LocalDate end = parseDate(endDate, LocalDate.now());
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        LocalDate rangeStart = start;
        LocalDate rangeEnd = end;

        UserStatsResponse totals = fetchOrderStats(rangeStart, rangeEnd);
        List<DashboardOrderResponse> orders = fetchOrders(authorization).stream()
                .filter(order -> isCountable(order.getStatus()))
                .filter(order -> isWithinRange(order, rangeStart, rangeEnd))
                .collect(Collectors.toList());

        RevenueStatisticResponse response = RevenueStatisticResponse.builder()
                .totalRevenue(defaultDecimal(totals.getTotalRevenue()))
                .totalOrders(defaultLong(totals.getTotalOrders()))
                .breakdown(buildDailyBreakdown(orders))
                .revenueByPaymentMethod(buildPaymentMethodBreakdown(orders))
                .categoryPerformance(List.of())
                .build();

        return ApiResponse.success(response);
    }

    private UserStatsResponse fetchOrderStats(LocalDate start, LocalDate end) {
        try {
            return orderClient.getOrderStats(
                    start.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    end.atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
            log.warn("Failed to fetch order revenue stats", e);
            return UserStatsResponse.builder()
                    .totalRevenue(BigDecimal.ZERO)
                    .totalOrders(0L)
                    .build();
        }
    }

    private List<DashboardOrderResponse> fetchOrders(String authorization) {
        Map<String, DashboardOrderResponse> orders = new LinkedHashMap<>();
        for (int pageNumber = 0; pageNumber < MAX_ORDER_PAGES; pageNumber++) {
            try {
                ApiResponse<SpringPageResponse<DashboardOrderResponse>> response =
                        orderClient.getAdminOrders(authorization, pageNumber, ORDER_PAGE_SIZE);
                SpringPageResponse<DashboardOrderResponse> page = response != null ? response.getResult() : null;
                List<DashboardOrderResponse> content = page != null && page.getContent() != null
                        ? page.getContent()
                        : List.of();

                content.stream()
                        .filter(order -> order.getId() != null)
                        .forEach(order -> orders.put(order.getId(), order));

                if (content.isEmpty() || page == null || pageNumber >= page.getTotalPages() - 1) {
                    break;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch admin orders for revenue statistics", e);
                break;
            }
        }
        return List.copyOf(orders.values());
    }

    private List<RevenueStatisticResponse.DailyRevenuePoint> buildDailyBreakdown(List<DashboardOrderResponse> orders) {
        Map<LocalDate, DailyCounter> daily = new LinkedHashMap<>();

        orders.stream()
                .sorted(Comparator.comparing(order -> parseOrderDate(order.getOrderDate()), Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(order -> {
                    LocalDateTime orderDate = parseOrderDate(order.getOrderDate());
                    if (orderDate == null) {
                        return;
                    }
                    DailyCounter counter = daily.computeIfAbsent(orderDate.toLocalDate(), ignored -> new DailyCounter());
                    counter.revenue = counter.revenue.add(defaultDecimal(order.getTotal()));
                    counter.orderCount++;
                });

        return daily.entrySet().stream()
                .map(entry -> RevenueStatisticResponse.DailyRevenuePoint.builder()
                        .date(entry.getKey().toString())
                        .revenue(entry.getValue().revenue)
                        .orderCount(entry.getValue().orderCount)
                        .build())
                .collect(Collectors.toList());
    }

    private List<RevenueStatisticResponse.PercentageDTO> buildPaymentMethodBreakdown(List<DashboardOrderResponse> orders) {
        Map<String, BigDecimal> revenueByMethod = new LinkedHashMap<>();
        orders.forEach(order -> {
            String method = order.getPaymentMethod() == null || order.getPaymentMethod().isBlank()
                    ? "Khac"
                    : order.getPaymentMethod();
            revenueByMethod.merge(method, defaultDecimal(order.getTotal()), BigDecimal::add);
        });

        BigDecimal total = revenueByMethod.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        return revenueByMethod.entrySet().stream()
                .map(entry -> RevenueStatisticResponse.PercentageDTO.builder()
                        .category(entry.getKey())
                        .value(entry.getValue()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(total, 1, RoundingMode.HALF_UP))
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isWithinRange(DashboardOrderResponse order, LocalDate start, LocalDate end) {
        LocalDateTime orderDate = parseOrderDate(order.getOrderDate());
        if (orderDate == null) {
            return false;
        }
        LocalDate date = orderDate.toLocalDate();
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private boolean isCountable(String status) {
        return status == null || !"CANCELLED".equalsIgnoreCase(status);
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private LocalDateTime parseOrderDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private static class DailyCounter {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orderCount;
    }
}
