package com.prakalpa.api.controllers;

import com.prakalpa.api.config.CustomUserDetails;
import com.prakalpa.api.config.JwtTokenProvider;
import com.prakalpa.api.dto.AuthResponse;
import com.prakalpa.api.dto.LoginRequest;
import com.prakalpa.api.models.Roles;
import com.prakalpa.api.models.Users;
import com.prakalpa.api.services.RoleServices;
import com.prakalpa.api.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    // Constructor Injection
    public UserController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserServices userServices;

    @Autowired
    private RoleServices roleServices;

    @GetMapping("/users/profile")
    public Users getCurrentUserProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userDetails.getUserDetails();
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> registerUser(@RequestBody Users user) {
        Roles roles = roleServices.findById(user.getRoles().getId());
        if (roles == null) {
            return ResponseEntity.badRequest().body("Error: Role not found with ID " + roles);
        }
        user.setRoles(roles);
        System.out.println(user.toString());
        user.setEmail(user.getUsername());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userServices.create(user);
        return ResponseEntity.ok("Registered successfully!");
    }


    @PostMapping("/auth/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            // 1. Authenticate username and password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // 2. Set authentication in Security Context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. Extract roles/authorities
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());


            // 3. Generate JWT Token
            String jwt = tokenProvider.generateToken(authentication, roles);

            // 4. Return token response
            return ResponseEntity.ok(new AuthResponse(jwt));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }
    }

    @GetMapping(path = {"/admin/users", "/users"})
    public List<Users> findAll(){
        return userServices.getUserList();
    }

    @GetMapping("/users/me") // Recommended endpoint for current logged-in user
    public ResponseEntity<Users> getLoggedInUser(Authentication authentication) {
        String currentUsername = authentication.getName();
        Users user = userServices.getUsers(currentUsername);
        return ResponseEntity.ok(user);
    }

    @PostMapping(path = "/users/create")
    public ResponseEntity<?> createUsers(@RequestBody Users users){
        Roles roles = roleServices.findById(users.getRoles().getId());
        if (roles == null) {
            return ResponseEntity.badRequest().body("Error: Role not found with ID " + roles);
        }
        users.setRoles(roles);
        users.setPassword(passwordEncoder.encode(users.getUsername()));
        userServices.create(users);
        return ResponseEntity.ok("Registered successfully!");
    }
}