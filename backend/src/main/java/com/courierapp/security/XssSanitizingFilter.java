package com.courierapp.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class XssSanitizingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest http) {
            String method = http.getMethod();
            String contentType = http.getContentType();
            // Only wrap JSON/form POST/PUT/PATCH — skip multipart (file uploads) and GET
            boolean isModifying = "POST".equalsIgnoreCase(method)
                    || "PUT".equalsIgnoreCase(method)
                    || "PATCH".equalsIgnoreCase(method);
            boolean isMultipart = contentType != null && contentType.startsWith("multipart/");
            if (isModifying && !isMultipart) {
                chain.doFilter(new XssRequestWrapper(http), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
