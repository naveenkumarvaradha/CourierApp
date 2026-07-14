package com.courierapp.booking.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
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
