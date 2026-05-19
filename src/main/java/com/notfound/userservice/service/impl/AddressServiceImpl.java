package com.notfound.userservice.service.impl;

import com.notfound.userservice.model.dto.request.CreateAddressRequest;
import com.notfound.userservice.model.dto.request.UpdateAddressRequest;
import com.notfound.userservice.model.dto.response.AddressDetailResponse;
import com.notfound.userservice.model.dto.response.AddressResponse;
import com.notfound.userservice.model.entity.Address;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.mapper.AddressMapper;
import com.notfound.userservice.repository.AddressRepository;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.exception.ResourceNotFoundException;
import com.notfound.userservice.service.AddressService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressServiceImpl implements AddressService {

    AddressRepository addressRepository;
    UserRepository userRepository;
    AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressResponse createAddress(CreateAddressRequest request) {
        User currentUser = getCurrentUser();

        Address address = addressMapper.toAddress(request);
        address.setUser(currentUser);

        Address savedAddress = addressRepository.save(address);
        return addressMapper.toAddressResponse(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID id, UpdateAddressRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        validateAddressOwnership(address);

        addressMapper.updateAddressFromRequest(request, address);
        Address updatedAddress = addressRepository.save(address);
        return addressMapper.toAddressResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        validateAddressOwnership(address);
        addressRepository.delete(address);
    }

    @Override
    public AddressResponse getAddressById(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        validateAddressOwnership(address);
        return addressMapper.toAddressResponse(address);
    }

    @Override
    public List<AddressResponse> getUserAddresses() {
        User currentUser = getCurrentUser();
        List<Address> addresses = addressRepository.findByUserId(currentUser.getId());
        return addressMapper.toAddressResponseList(addresses);
    }

    @Override
    public AddressDetailResponse getAddressDetail(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc người dùng này"));
        return addressMapper.toAddressDetailResponse(address);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
    }

    private void validateAddressOwnership(Address address) {
        User currentUser = getCurrentUser();
        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Không có quyền truy cập địa chỉ này");
        }
    }
}
