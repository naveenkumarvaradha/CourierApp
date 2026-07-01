package com.courierapp.service;

import com.courierapp.dto.auth.CurrentUserResponse;
import com.courierapp.dto.auth.LoginRequest;
import com.courierapp.dto.auth.RefreshRequest;
import com.courierapp.dto.auth.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(RefreshRequest request);
    CurrentUserResponse currentUser(String username);
}
