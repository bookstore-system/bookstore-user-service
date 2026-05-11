package com.notfound.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.userservice.exception.GlobalExceptionHandler;
import com.notfound.userservice.model.dto.request.CreateAddressRequest;
import com.notfound.userservice.model.dto.response.AddressResponse;
import com.notfound.userservice.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressControllerTest {

    private MockMvc mockMvc;
    private AddressService addressService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        addressService = mock(AddressService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        AddressController controller = new AddressController(addressService);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                        .build();
    }

    @Test
    void createAddress_validRequest_returnsApiResponse() throws Exception {
        UUID addressId = UUID.randomUUID();
        AddressResponse serviceResponse =
                AddressResponse.builder()
                        .id(addressId)
                        .recipientName("Nguyễn Văn A")
                        .phoneNumber("0123456789")
                        .street("123 Street")
                        .ward("Phường 1")
                        .district("Quận 1")
                        .province("Hồ Chí Minh")
                        .latitude(new BigDecimal("10.123"))
                        .longitude(new BigDecimal("106.123"))
                        .provinceId(202)
                        .districtId(1461)
                        .wardCode("21301")
                        .build();

        when(addressService.createAddress(any(CreateAddressRequest.class))).thenReturn(serviceResponse);

        CreateAddressRequest request =
                CreateAddressRequest.builder()
                        .recipientName("Nguyễn Văn A")
                        .phoneNumber("0123456789")
                        .street("123 Street")
                        .ward("Phường 1")
                        .district("Quận 1")
                        .province("Hồ Chí Minh")
                        .latitude(new BigDecimal("10.123"))
                        .longitude(new BigDecimal("106.123"))
                        .provinceId(202)
                        .districtId(1461)
                        .wardCode("21301")
                        .build();

        mockMvc.perform(
                        post("/api/v1/addresses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Thêm địa chỉ thành công"))
                .andExpect(jsonPath("$.result.id").value(addressId.toString()));
    }

    @Test
    void createAddress_invalidRequest_returns400ApiError() throws Exception {
        // missing recipientName, phoneNumber, provinceId, wardCode...
        mockMvc.perform(post("/api/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void getUserAddresses_returnsList() throws Exception {
        when(addressService.getUserAddresses())
                .thenReturn(List.of(AddressResponse.builder().id(UUID.randomUUID()).build()));

        mockMvc.perform(get("/api/v1/addresses/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isArray());
    }
}

