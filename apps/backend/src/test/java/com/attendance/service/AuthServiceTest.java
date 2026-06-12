package com.attendance.service;

import com.attendance.dto.auth.*;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.entity.UserRole;
import com.attendance.exception.AuthenticationFailedException;
import com.attendance.repository.UserRepository;
import com.attendance.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest extends ServiceTestBase {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private AuthService authService;

    private User activeUser;
    private final String RAW_PASSWORD = "Admin@2026";
    private final String ENCODED_PASSWORD = "$2a$10$encodedPasswordHash";

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("admin@company.com");
        activeUser.setName("系統管理員");
        activeUser.setRole(UserRole.ADMIN);
        activeUser.setPassword(ENCODED_PASSWORD);
        activeUser.setIsActive(true);
        activeUser.setMustChangePassword(true);
        activeUser.setDepartment(new Department(1L, "資訊部", null));
    }

    // ─── login ────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("成功登入 — 回傳 access + refresh token")
        void loginSuccess() {
            LoginRequest req = new LoginRequest();
            req.setEmail("admin@company.com");
            req.setPassword(RAW_PASSWORD);

            when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(1L, "admin@company.com", "ADMIN"))
                    .thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(1L, "admin@company.com", "ADMIN"))
                    .thenReturn("refresh-token");

            LoginResponse resp = authService.login(req);

            assertThat(resp.getAccessToken()).isEqualTo("access-token");
            assertThat(resp.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(resp.getUser().getEmail()).isEqualTo("admin@company.com");
            assertThat(resp.getUser().getRole()).isEqualTo("ADMIN");
            assertThat(resp.getUser().isMustChangePassword()).isTrue();
        }

        @Test
        @DisplayName("帳號不存在 — 拋出 AuthenticationFailedException (401)")
        void loginFail_userNotFound() {
            LoginRequest req = new LoginRequest();
            req.setEmail("nobody@company.com");
            req.setPassword(RAW_PASSWORD);

            when(userRepository.findByEmail("nobody@company.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("帳號或密碼錯誤");
        }

        @Test
        @DisplayName("帳號已停用 — 拋出 AuthenticationFailedException (401)")
        void loginFail_userDeactivated() {
            activeUser.setIsActive(false);
            LoginRequest req = new LoginRequest();
            req.setEmail("admin@company.com");
            req.setPassword(RAW_PASSWORD);

            when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("帳號已停用");
        }

        @Test
        @DisplayName("密碼錯誤 — 拋出 AuthenticationFailedException (401)")
        void loginFail_wrongPassword() {
            LoginRequest req = new LoginRequest();
            req.setEmail("admin@company.com");
            req.setPassword("WrongPassword");

            when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("WrongPassword", ENCODED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("帳號或密碼錯誤");
        }
    }

    // ─── refresh ──────────────────────────────────────────────

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("成功刷新 — 回傳新 token pair")
        void refreshSuccess() {
            RefreshRequest req = new RefreshRequest();
            req.setRefreshToken("valid-refresh-token");

            when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.getUserId("valid-refresh-token")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            when(jwtTokenProvider.generateAccessToken(1L, "admin@company.com", "ADMIN"))
                    .thenReturn("new-access");
            when(jwtTokenProvider.generateRefreshToken(1L, "admin@company.com", "ADMIN"))
                    .thenReturn("new-refresh");

            LoginResponse resp = authService.refresh(req);

            assertThat(resp.getAccessToken()).isEqualTo("new-access");
            assertThat(resp.getRefreshToken()).isEqualTo("new-refresh");
            assertThat(resp.getUser().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("無效 refresh token — 拋出異常")
        void refreshFail_invalidToken() {
            RefreshRequest req = new RefreshRequest();
            req.setRefreshToken("bad-token");

            when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(req))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("無效的 refresh token");
        }

        @Test
        @DisplayName("使用者不存在 — 拋出異常")
        void refreshFail_userNotFound() {
            RefreshRequest req = new RefreshRequest();
            req.setRefreshToken("valid-refresh-token");

            when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.getUserId("valid-refresh-token")).thenReturn(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("使用者不存在");
        }
    }

    // ─── changePassword ──────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("成功變更密碼 — mustChangePassword 設為 false")
        void changePasswordSuccess() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword(RAW_PASSWORD);
            req.setNewPassword("NewPass@2026");

            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.encode("NewPass@2026")).thenReturn("$2a$10$newEncoded");

            authService.changePassword(1L, req);

            verify(userRepository).save(argThat(user ->
                    !user.getMustChangePassword()));
        }

        @Test
        @DisplayName("使用者不存在 — 拋出異常")
        void changePasswordFail_userNotFound() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword(RAW_PASSWORD);
            req.setNewPassword("NewPass@2026");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(999L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("使用者不存在");
        }

        @Test
        @DisplayName("舊密碼錯誤 — 拋出異常")
        void changePasswordFail_wrongOldPassword() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("WrongOldPass");
            req.setNewPassword("NewPass@2026");

            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("WrongOldPass", ENCODED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("舊密碼不正確");
        }
    }
}
