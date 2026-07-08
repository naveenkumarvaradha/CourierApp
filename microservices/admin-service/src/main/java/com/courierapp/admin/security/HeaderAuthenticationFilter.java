package com.courierapp.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String userId    = request.getHeader("X-User-Id");
        String username  = request.getHeader("X-Username");
        String companyId = request.getHeader("X-Company-Id");
        String roles     = request.getHeader("X-Roles");

        if (username != null && !username.isBlank()) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (roles != null) {
                Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(r -> !r.isBlank())
                        .forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
            }
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(Map.of(
                    "userId",    userId    != null ? userId    : "",
                    "companyId", companyId != null ? companyId : ""));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
