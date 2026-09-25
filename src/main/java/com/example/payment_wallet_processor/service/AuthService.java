package com.example.payment_wallet_processor.service;

import com.example.payment_wallet_processor.dto.LoginRequest;
import com.example.payment_wallet_processor.dto.RegisterRequest;
import com.example.payment_wallet_processor.entity.Role;
import com.example.payment_wallet_processor.entity.User;
import com.example.payment_wallet_processor.repostiory.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest registerRequest) {

        // 1. Check if email already exists
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new RuntimeException("Email already exists");
        }

        // 2. Hash the password
        String encodedPassword =
                passwordEncoder.encode(registerRequest.password());

        // 3. Create the user
        User user = new User(
                registerRequest.name(),
                registerRequest.email(),
                encodedPassword,
                Role.USER
        );

        // 4. Save the user
        userRepository.save(user);
    }

    public User login(LoginRequest loginRequest) {

//        Find user by email
        User user = userRepository
                .findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

//        Check password
        boolean passwordMatches = passwordEncoder.matches(loginRequest.password(), user.getPassword());

//        Reject if password is in correct

        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

//        Login succesfully

        return user;
    }
}