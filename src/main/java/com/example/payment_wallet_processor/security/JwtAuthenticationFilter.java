package com.example.payment_wallet_processor.security;

import com.example.payment_wallet_processor.entity.User;
import com.example.payment_wallet_processor.repostiory.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Check Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT
        String token = authHeader.substring(7);

        // Validate JWT
        if (jwtService.isTokenValid(token)) {

            // Extract email from JWT
            String email = jwtService.extractEmail(token);

            // Check email and existing authentication
            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // Find user from database
                Optional<User> userOptional =
                        userRepository.findByEmail(email);

                if (userOptional.isPresent()) {

                    User user = userOptional.get();

                    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name()

                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of(authority)
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    System.out.println("================================");
                    System.out.println("Authenticated Email: " + email);
                    System.out.println("User Role: " + user.getRole());
                    System.out.println("Authorities: " + authentication.getAuthorities());
                    System.out.println("================================");
                }
            }
        }

        // Continue request
        filterChain.doFilter(request, response);
    }
}