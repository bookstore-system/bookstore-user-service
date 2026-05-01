package com.notfound.userservice.model.mapper;

import com.notfound.userservice.model.dto.request.CreateAddressRequest;
import com.notfound.userservice.model.dto.request.UpdateAddressRequest;
import com.notfound.userservice.model.dto.response.AddressResponse;
import com.notfound.userservice.model.entity.Address;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(source = "provinceId", target = "provinceId")
    @Mapping(source = "districtId", target = "districtId")
    @Mapping(source = "wardCode", target = "wardCode")
    AddressResponse toAddressResponse(Address address);

    List<AddressResponse> toAddressResponseList(List<Address> addresses);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Address toAddress(CreateAddressRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateAddressFromRequest(UpdateAddressRequest request, @MappingTarget Address address);
}
