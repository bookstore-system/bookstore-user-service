package com.notfound.userservice.controller;

import com.notfound.userservice.model.dto.request.CreateAddressRequest;
import com.notfound.userservice.model.dto.request.UpdateAddressRequest;
import com.notfound.userservice.model.dto.response.AddressResponse;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.service.AddressService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller xử lý các chức năng liên quan đến địa chỉ người dùng
 * Cho phép người dùng quản lý danh sách địa chỉ giao hàng
 */
@RestController
@RequestMapping("/api/v1/users/addresses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressController {

    AddressService addressService;

    /**
     * Thêm địa chỉ mới cho người dùng hiện tại
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<AddressResponse> createAddress(@Valid @RequestBody CreateAddressRequest request) {
        AddressResponse address = addressService.createAddress(request);
        return ApiResponse.<AddressResponse>builder()
                .code(1000)
                .message("Thêm địa chỉ thành công")
                .result(address)
                .build();
    }

    /**
     * Cập nhật thông tin địa chỉ
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<AddressResponse> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAddressRequest request) {
        AddressResponse address = addressService.updateAddress(id, request);
        return ApiResponse.<AddressResponse>builder()
                .code(1000)
                .message("Cập nhật địa chỉ thành công")
                .result(address)
                .build();
    }

    /**
     * Xóa địa chỉ
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<Void> deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa địa chỉ thành công")
                .build();
    }

    /**
     * Lấy thông tin chi tiết của một địa chỉ
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<AddressResponse> getAddressById(@PathVariable UUID id) {
        AddressResponse address = addressService.getAddressById(id);
        return ApiResponse.<AddressResponse>builder()
                .code(1000)
                .message("Lấy thông tin địa chỉ thành công")
                .result(address)
                .build();
    }

    /**
     * Lấy danh sách tất cả địa chỉ của người dùng hiện tại
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<List<AddressResponse>> getUserAddresses() {
        List<AddressResponse> addresses = addressService.getUserAddresses();
        return ApiResponse.<List<AddressResponse>>builder()
                .code(1000)
                .message("Lấy danh sách địa chỉ thành công")
                .result(addresses)
                .build();
    }
}
