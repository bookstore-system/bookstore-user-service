package com.notfound.userservice.service;

import com.notfound.userservice.model.dto.request.CreateAddressRequest;
import com.notfound.userservice.model.dto.request.UpdateAddressRequest;
import com.notfound.userservice.model.dto.response.AddressDetailResponse;
import com.notfound.userservice.model.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    AddressResponse createAddress(CreateAddressRequest request);

    AddressResponse updateAddress(UUID id, UpdateAddressRequest request);

    void deleteAddress(UUID id);

    AddressResponse getAddressById(UUID id);

    List<AddressResponse> getUserAddresses();

    AddressDetailResponse getAddressDetail(UUID userId, UUID addressId);
}
