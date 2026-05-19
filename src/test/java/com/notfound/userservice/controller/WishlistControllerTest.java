package com.notfound.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.userservice.exception.GlobalExceptionHandler;
import com.notfound.userservice.model.dto.request.AddBookToWishlistRequest;
import com.notfound.userservice.model.dto.response.WishlistResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistControllerTest {

    private MockMvc mockMvc;
    private WishlistService wishlistService;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        wishlistService = mock(WishlistService.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        WishlistController controller = new WishlistController(wishlistService, userRepository);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                        .build();
    }

    @Test
    void getMyWishlist_returnsWishlist() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(User.builder().id(userId).username("testuser").build()));
        when(wishlistService.getMyWishlist(userId)).thenReturn(WishlistResponse.builder().build());

        mockMvc.perform(get("/api/v1/wishlist")
                        .principal(new UsernamePasswordAuthenticationToken("testuser", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").exists());
    }

    @Test
    void addBookToWishlist_valid_returnsWishlist() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(User.builder().id(userId).username("testuser").build()));
        when(wishlistService.addBookToWishlist(any(UUID.class), any(AddBookToWishlistRequest.class)))
                .thenReturn(WishlistResponse.builder().build());

        AddBookToWishlistRequest request = new AddBookToWishlistRequest();
        request.setBookId(UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/wishlist/add")
                                .principal(new UsernamePasswordAuthenticationToken("testuser", "N/A"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Thêm sách vào wishlist thành công"));
    }

    @Test
    void removeBookFromWishlist_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(User.builder().id(userId).username("testuser").build()));

        mockMvc.perform(
                        delete("/api/v1/wishlist/remove/{bookId}", bookId)
                                .principal(new UsernamePasswordAuthenticationToken("testuser", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đã xóa sách khỏi wishlist thành công"));
    }

    @Test
    void clearWishlist_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(User.builder().id(userId).username("testuser").build()));

        mockMvc.perform(
                        delete("/api/v1/wishlist/clear")
                                .principal(new UsernamePasswordAuthenticationToken("testuser", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đã xóa toàn bộ wishlist thành công"));
    }
}

