package com.notfound.userservice.controller;

import com.notfound.userservice.client.BookClient;
import com.notfound.userservice.client.OrderClient;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.BookServiceApiResponse;
import com.notfound.userservice.model.dto.response.DashboardBookResponse;
import com.notfound.userservice.model.dto.response.DashboardOrderResponse;
import com.notfound.userservice.model.dto.response.DashboardPerformanceResponse;
import com.notfound.userservice.model.dto.response.DashboardRecentOrdersResponse;
import com.notfound.userservice.model.dto.response.DashboardSalesTrendResponse;
import com.notfound.userservice.model.dto.response.DashboardStatsResponse;
import com.notfound.userservice.model.dto.response.DashboardTopBooksResponse;
import com.notfound.userservice.model.dto.response.DashboardTopCategoriesResponse;
import com.notfound.userservice.model.dto.response.SpringPageResponse;
import com.notfound.userservice.model.dto.response.UserStatsResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.service.UserService;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminDashboardController {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final UserService userService;
    private final UserRepository userRepository;
    private final BookClient bookClient;
    private final OrderClient orderClient;

    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStats(
            @RequestHeader("Authorization") String authorization) {
        UserStatsResponse userStats = userService.getUserStatistics();
        List<DashboardBookResponse> books = fetchAdminBooks(authorization);

        long totalBooksInStock = books.stream()
                .filter(book -> value(book.getStockQuantity()) > 0)
                .count();
        long lowStockCount = books.stream()
                .filter(book -> value(book.getStockQuantity()) > 0 && value(book.getStockQuantity()) <= LOW_STOCK_THRESHOLD)
                .count();

        DashboardStatsResponse response = DashboardStatsResponse.builder()
                .totalRevenue(defaultDecimal(userStats.getTotalRevenue()))
                .revenueGrowth(BigDecimal.ZERO)
                .totalOrders(defaultLong(userStats.getTotalOrders()))
                .ordersGrowth(BigDecimal.ZERO)
                .totalBooksInStock(totalBooksInStock)
                .lowStockCount(lowStockCount)
                .activeCustomers(defaultLong(userStats.getActiveUsers()))
                .newCustomers(userRepository.countByCreatedAtAfter(LocalDate.now().withDayOfMonth(1).atStartOfDay()))
                .build();

        return ApiResponse.success(response);
    }

    @GetMapping("/sales-trend")
    public ApiResponse<DashboardSalesTrendResponse> getSalesTrend(
            @RequestParam(defaultValue = "6") int months) {
        int safeMonths = Math.max(1, Math.min(months, 12));
        List<DashboardSalesTrendResponse.SalesTrendData> data = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = safeMonths - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);
            UserStatsResponse orderStats = fetchOrderStats(start, end);

            data.add(DashboardSalesTrendResponse.SalesTrendData.builder()
                    .month(month.toString())
                    .monthName("T" + month.getMonthValue())
                    .sales(defaultDecimal(orderStats.getTotalRevenue()))
                    .orders(defaultLong(orderStats.getTotalOrders()))
                    .customers(0L)
                    .build());
        }

        return ApiResponse.success(DashboardSalesTrendResponse.builder().data(data).build());
    }

    @GetMapping("/top-categories")
    public ApiResponse<DashboardTopCategoriesResponse> getTopCategories(
            @RequestHeader("Authorization") String authorization) {
        Map<String, CategoryCounter> counters = new LinkedHashMap<>();

        for (DashboardBookResponse book : fetchAdminBooks(authorization)) {
            if (book.getCategories() == null) {
                continue;
            }
            for (DashboardBookResponse.CategoryInfo category : book.getCategories()) {
                if (category == null || category.getId() == null) {
                    continue;
                }
                counters.computeIfAbsent(category.getId(), id -> new CategoryCounter(id, category.getName()))
                        .count++;
            }
        }

        long total = counters.values().stream().mapToLong(counter -> counter.count).sum();
        List<DashboardTopCategoriesResponse.TopCategory> categories = counters.values().stream()
                .sorted(Comparator.comparingLong((CategoryCounter counter) -> counter.count).reversed())
                .limit(5)
                .map(counter -> DashboardTopCategoriesResponse.TopCategory.builder()
                        .categoryId(counter.id)
                        .categoryName(counter.name)
                        .percentage(total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(counter.count * 100.0 / total).setScale(1, RoundingMode.HALF_UP))
                        .totalSales(BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(DashboardTopCategoriesResponse.builder().categories(categories).build());
    }

    @GetMapping("/performance")
    public ApiResponse<DashboardPerformanceResponse> getPerformance() {
        return ApiResponse.success(DashboardPerformanceResponse.builder()
                .conversionRate(DashboardPerformanceResponse.Metric.builder().current(0D).target(5D).build())
                .satisfactionRate(DashboardPerformanceResponse.Metric.builder().current(0D).target(90D).build())
                .build());
    }

    @GetMapping("/top-selling-books")
    public ApiResponse<DashboardTopBooksResponse> getTopSellingBooks(
            @RequestParam(defaultValue = "5") int limit) {
        List<DashboardTopBooksResponse.TopSellingBook> books = fetchBestSellingBooks(Math.max(1, Math.min(limit, 20))).stream()
                .map(book -> DashboardTopBooksResponse.TopSellingBook.builder()
                        .id(book.getId())
                        .title(book.getTitle())
                        .authorNames(book.getAuthorNames() == null ? List.of() : book.getAuthorNames())
                        .categoryNames(extractCategoryNames(book))
                        .price(defaultDouble(book.getPrice()))
                        .discountPrice(defaultDouble(book.getDiscountPrice()))
                        .averageRating(defaultDouble(book.getAverageRating()))
                        .reviewCount(value(book.getReviewCount()))
                        .soldQuantity(value(book.getTotalOrders()))
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(DashboardTopBooksResponse.builder().books(books).build());
    }

    @GetMapping("/recent-orders")
    public ApiResponse<DashboardRecentOrdersResponse> getRecentOrders(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "4") int limit) {
        List<DashboardRecentOrdersResponse.RecentOrder> orders = fetchRecentOrders(authorization, Math.max(1, Math.min(limit, 20))).stream()
                .map(order -> DashboardRecentOrdersResponse.RecentOrder.builder()
                        .id(order.getId())
                        .orderCode(toOrderCode(order.getId()))
                        .customerName(resolveCustomerName(order.getCustomerId()))
                        .total(defaultDecimal(order.getTotal()))
                        .orderDate(order.getOrderDate())
                        .status(order.getStatus())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(DashboardRecentOrdersResponse.builder().orders(orders).build());
    }

    private UserStatsResponse fetchOrderStats(LocalDateTime start, LocalDateTime end) {
        try {
            return orderClient.getOrderStats(start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
            log.warn("Failed to fetch monthly order stats", e);
            return UserStatsResponse.builder().totalRevenue(BigDecimal.ZERO).totalOrders(0L).build();
        }
    }

    private List<DashboardBookResponse> fetchAdminBooks(String authorization) {
        try {
            BookServiceApiResponse<SpringPageResponse<DashboardBookResponse>> response = bookClient.getAdminBooks(authorization, 0, 1000);
            SpringPageResponse<DashboardBookResponse> page = response != null ? response.getData() : null;
            return page != null && page.getContent() != null ? page.getContent() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch admin books for dashboard", e);
            return List.of();
        }
    }

    private List<DashboardBookResponse> fetchBestSellingBooks(int limit) {
        try {
            BookServiceApiResponse<List<DashboardBookResponse>> response = bookClient.getBestSellingBooks(limit);
            return response != null && response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch best selling books for dashboard", e);
            return List.of();
        }
    }

    private List<DashboardOrderResponse> fetchRecentOrders(String authorization, int limit) {
        try {
            ApiResponse<SpringPageResponse<DashboardOrderResponse>> response = orderClient.getAdminOrders(authorization, 0, limit);
            SpringPageResponse<DashboardOrderResponse> page = response != null ? response.getResult() : null;
            return page != null && page.getContent() != null ? page.getContent() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch recent orders for dashboard", e);
            return List.of();
        }
    }

    private String resolveCustomerName(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return "Khach hang";
        }
        try {
            UUID id = UUID.fromString(customerId);
            return userRepository.findById(id)
                    .map(this::displayName)
                    .orElse("Khach hang");
        } catch (IllegalArgumentException e) {
            return "Khach hang";
        }
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }

    private List<String> extractCategoryNames(DashboardBookResponse book) {
        if (book.getCategories() == null) {
            return List.of();
        }
        return book.getCategories().stream()
                .map(DashboardBookResponse.CategoryInfo::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
    }

    private String toOrderCode(String id) {
        if (id == null || id.length() <= 8) {
            return id;
        }
        return "#" + id.substring(0, 8).toUpperCase();
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal defaultDecimal(Number value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value.doubleValue());
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private Double defaultDouble(Double value) {
        return value == null ? 0D : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private static class CategoryCounter {
        private final String id;
        private final String name;
        private long count;

        private CategoryCounter(String id, String name) {
            this.id = id;
            this.name = name == null || name.isBlank() ? "Khac" : name;
        }
    }
}
