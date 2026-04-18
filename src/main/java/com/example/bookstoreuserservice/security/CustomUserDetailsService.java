package com.example.bookstoreuserservice.security;

import com.example.bookstoreuserservice.user.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.example.bookstoreuserservice.user.User entity =
                userRepository
                        .findByEmailIgnoreCase(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(entity.getEmail())
                .password(entity.getPasswordHash())
                .roles("USER")
                .build();
    }
}
