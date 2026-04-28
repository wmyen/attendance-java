package com.attendance.service;

import com.attendance.dto.auth.*;
import com.attendance.entity.User;
import com.attendance.repository.UserRepository;
import com.attendance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("帳號或密碼錯誤"));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("帳號已停用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("帳號或密碼錯誤");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return new LoginResponse(accessToken, refreshToken,
                new LoginResponse.UserInfo(
                        user.getId(), user.getEmail(), user.getName(),
                        user.getRole().name(), user.getMustChangePassword()));
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new IllegalArgumentException("無效的 refresh token");
        }

        Long userId = jwtTokenProvider.getUserId(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return new LoginResponse(accessToken, refreshToken,
                new LoginResponse.UserInfo(
                        user.getId(), user.getEmail(), user.getName(),
                        user.getRole().name(), user.getMustChangePassword()));
    }

    @Transactional
    public void changePassword(@NonNull Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("舊密碼不正確");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
