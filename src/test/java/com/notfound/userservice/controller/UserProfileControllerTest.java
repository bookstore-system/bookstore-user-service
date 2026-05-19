package com.notfound.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.userservice.exception.GlobalExceptionHandler;
import com.notfound.userservice.model.dto.request.UpdateProfileRequest;
import com.notfound.userservice.model.dto.response.AddressDetailResponse;
import com.notfound.userservice.model.dto.response.ContactInfoResponse;
import com.notfound.userservice.model.dto.response.UserBasicInfoResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.service.AddressService;
import com.notfound.userservice.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerTest {

    private MockMvc mockMvc;
    private UserService userService;
    private AddressService addressService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        addressService = mock(AddressService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        UserProfileController controller = new UserProfileController(userService, addressService);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                        .build();
    }

    @Test
    void me_returnsUser() throws Exception {
        when(userService.getUserByUsername("u")).thenReturn(UserResponse.builder().username("u").build());

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(new UsernamePasswordAuthenticationToken("u", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.username").value("u"));
    }

    @Test
    void profile_returnsUser() throws Exception {
        when(userService.getUserByUsername("u")).thenReturn(UserResponse.builder().username("u").build());

        mockMvc.perform(get("/api/v1/users/profile")
                        .principal(new UsernamePasswordAuthenticationToken("u", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.username").value("u"));
    }

    @Test
    void updateProfile_putMultipart_callsService() throws Exception {
        when(userService.updateProfile(eq("u"), any(UpdateProfileRequest.class), isNull()))
                .thenReturn(UserResponse.builder().username("u").fullName("New Name").build());

        MockMultipartHttpServletRequestBuilder req =
                multipart("/api/v1/users/profile")
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        })
                        .principal(new UsernamePasswordAuthenticationToken("u", "N/A"))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("fullName", "New Name");

        mockMvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.fullName").value("New Name"));
    }

    @Test
    void updateProfile_putMultipart_withAvatarFile_passesNonEmptyFileToService() throws Exception {
        MockMultipartFile avatar =
                new MockMultipartFile("avatar", "a.png", "image/png", new byte[] {1, 2, 3});
        when(userService.updateProfile(eq("u"), any(UpdateProfileRequest.class), any(MultipartFile.class)))
                .thenReturn(UserResponse.builder().username("u").build());

        MockMultipartHttpServletRequestBuilder req =
                multipart("/api/v1/users/profile")
                        .file(avatar)
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        })
                        .principal(new UsernamePasswordAuthenticationToken("u", "N/A"))
                        .param("fullName", "With Avatar");

        mockMvc.perform(req).andExpect(status().isOk());

        ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(userService).updateProfile(eq("u"), any(UpdateProfileRequest.class), fileCaptor.capture());
        Assertions.assertFalse(fileCaptor.getValue().isEmpty());
    }

    @Test
    void contactInfo_returnsResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getUserContactInfo(userId))
                .thenReturn(ContactInfoResponse.builder().userId(userId).email("u@example.com").build());

        mockMvc.perform(get("/api/v1/users/{userId}/contact-info", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value(userId.toString()));
    }

    @Test
    void addressDetail_returnsResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(addressService.getAddressDetail(userId, addressId))
                .thenReturn(AddressDetailResponse.builder().id(addressId).build());

        mockMvc.perform(get("/api/v1/users/{userId}/addresses/{addressId}", userId, addressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(addressId.toString()));
    }

    @Test
    void basicInfo_returnsResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getUserBasicInfo(userId))
                .thenReturn(UserBasicInfoResponse.builder().userId(userId).displayName("U").build());

        mockMvc.perform(get("/api/v1/users/{userId}/basic-info", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value(userId.toString()))
                .andExpect(jsonPath("$.result.displayName").value("U"));
    }
}

