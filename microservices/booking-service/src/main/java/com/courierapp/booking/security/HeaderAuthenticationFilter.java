package com.courierapp.booking.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedSecret = request.getHeader("X-Internal-Auth");
        if (providedSecret == null || !MessageDigest.isEqual(
                providedSecret.getBytes(StandardCharsets.UTF_8), internalSecret.getBytes(StandardCharsets.UTF_8))) {
            // Identity headers were not signed by the gateway — proceed unauthenticated so
            // downstream authorization rejects the request rather than trusting spoofed headers.
            chain.doFilter(request, response);
            return;
        }

        String username = request.getHeader("X-Username");
        String userId = request.getHeader("X-User-Id");
        String companyId = request.getHeader("X-Company-Id");
        String rolesHeader = request.getHeader("X-Roles");

        if (username != null && !username.isBlank()) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (rolesHeader != null && !rolesHeader.isBlank()) {
                Arrays.stream(rolesHeader.split(",")).map(String::trim).filter(r -> !r.isBlank())
                        .forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
            }
            var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(Map.of("userId", userId != null ? userId : "", "companyId", companyId != null ? companyId : ""));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
