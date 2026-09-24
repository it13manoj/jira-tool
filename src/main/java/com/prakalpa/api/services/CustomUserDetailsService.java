package com.prakalpa.api.services;

import com.prakalpa.api.config.CustomUserDetails;
import com.prakalpa.api.repository.UserRepository;
import com.prakalpa.api.models.Users;
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
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        System.out.println("Loaded DB User: " + user.getUsername());
        System.out.println("Loaded DB Hash: " + user.getPassword());

        return new CustomUserDetails(user);
    }
}
