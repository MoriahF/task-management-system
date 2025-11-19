package com.taskmanagement.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.taskmanagement.model.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CognitoJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (token != null) {
                authenticateUser(request, token);
            }
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(HttpServletRequest request, String token) throws Exception {
        DecodedJWT decodedJWT = jwtTokenValidator.validateToken(token);

        String cognitoSub = jwtTokenValidator.getSubject(decodedJWT);
        String email = jwtTokenValidator.getEmail(decodedJWT);
        String roleString = jwtTokenValidator.getRole(decodedJWT);
        String name = jwtTokenValidator.getName(decodedJWT);

        UserRole role = UserRole.fromString(roleString);
        log.debug("Authenticated user: {} with role: {}", email, role);

        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("cognitoSub", cognitoSub);
        userDetails.put("email", email);
        userDetails.put("role", role);
        userDetails.put("name", name);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role.getAuthority()))
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }


    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}