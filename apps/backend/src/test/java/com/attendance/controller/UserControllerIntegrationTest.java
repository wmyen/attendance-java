package com.attendance.controller;

import com.attendance.dto.auth.LoginRequest;
import com.attendance.dto.auth.LoginResponse;
import com.attendance.dto.user.UserCreateRequest;
import com.attendance.dto.user.UserUpdateRequest;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.entity.UserRole;
import com.attendance.repository.DepartmentRepository;
import com.attendance.repository.UserRepository;
import com.attendance.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 整合測試。
 * 驗證 ADMIN 角色 CRUD 操作 + EMPLOYEE 角色權限隔離。
 */
@AutoConfigureMockMvc
class UserControllerIntegrationTest extends com.attendance.IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private Department testDept;
    private User adminUser;
    private User employeeUser;
    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        testDept = new Department();
        testDept.setName("測試部");
        testDept = departmentRepository.save(testDept);

        // ADMIN 使用者
        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setName("管理員");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setDepartment(testDept);
        adminUser.setPassword(passwordEncoder.encode("Admin@2026"));
        adminUser.setIsActive(true);
        adminUser.setMustChangePassword(false);
        adminUser = userRepository.save(adminUser);

        // EMPLOYEE 使用者
        employeeUser = new User();
        employeeUser.setEmail("employee@test.com");
        employeeUser.setName("員工");
        employeeUser.setRole(UserRole.EMPLOYEE);
        employeeUser.setDepartment(testDept);
        employeeUser.setManager(adminUser);
        employeeUser.setPassword(passwordEncoder.encode("Emp@2026"));
        employeeUser.setIsActive(true);
        employeeUser.setMustChangePassword(false);
        employeeUser = userRepository.save(employeeUser);

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
        employeeToken = jwtTokenProvider.generateAccessToken(
                employeeUser.getId(), employeeUser.getEmail(), employeeUser.getRole().name());
    }

    // ─── GET /api/v1/users ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/users")
    class ListUsersTests {

        @Test
        @DisplayName("ADMIN 列出使用者 — 回傳 200 + 分頁")
        void listAsAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(5)); // 3 種子 + 2 測試
        }

        @Test
        @DisplayName("EMPLOYEE 列出使用者 — 回傳 403（權限不足）")
        void listAsEmployee_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("帶搜尋條件列使用者")
        void listWithSearch() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .param("search", "管理員")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2)); // 種子「系統管理員」+ 測試「管理員」
        }
    }

    // ─── GET /api/v1/users/{id} ─────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserTests {

        @Test
        @DisplayName("ADMIN 取得單一使用者 — 回傳 200")
        void getUserSuccess() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", employeeUser.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("employee@test.com"))
                    .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                    .andExpect(jsonPath("$.deptName").value("測試部"))
                    .andExpect(jsonPath("$.managerName").value("管理員"));
        }

        @Test
        @DisplayName("EMPLOYEE 取得使用者 — 回傳 403")
        void getUserAsEmployee_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", adminUser.getId())
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── POST /api/v1/users ──────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/users")
    class CreateUserTests {

        @Test
        @DisplayName("ADMIN 建立使用者 — 回傳 200")
        void createUserSuccess() throws Exception {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("newuser@test.com");
            req.setName("新員工");
            req.setRole("EMPLOYEE");
            req.setDeptId(testDept.getId());
            req.setManagerId(adminUser.getId());

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("newuser@test.com"))
                    .andExpect(jsonPath("$.mustChangePassword").value(true))
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        @DisplayName("Email 重複 — 回傳 400")
        void createUserDuplicateEmail() throws Exception {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("employee@test.com");
            req.setName("重複Email");
            req.setRole("EMPLOYEE");

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EMPLOYEE 建立使用者 — 回傳 403")
        void createUserAsEmployee_forbidden() throws Exception {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("new@test.com");
            req.setName("新員工");
            req.setRole("EMPLOYEE");

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("缺少必填欄位 — 回傳 400 驗證錯誤")
        void createUserMissingFields() throws Exception {
            UserCreateRequest req = new UserCreateRequest();
            // 不填 email、name、role

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── PUT /api/v1/users/{id} ──────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
    class UpdateUserTests {

        @Test
        @DisplayName("ADMIN 更新使用者 — 回傳 200")
        void updateUserSuccess() throws Exception {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setName("更新名稱");
            req.setRole("MANAGER");

            mockMvc.perform(put("/api/v1/users/{id}", employeeUser.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("更新名稱"))
                    .andExpect(jsonPath("$.role").value("MANAGER"));
        }

        @Test
        @DisplayName("EMPLOYEE 更新使用者 — 回傳 403")
        void updateUserAsEmployee_forbidden() throws Exception {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setName("嘗試更新");

            mockMvc.perform(put("/api/v1/users/{id}", adminUser.getId())
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── DELETE /api/v1/users/{id} ───────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeactivateUserTests {

        @Test
        @DisplayName("ADMIN 停用使用者 — 回傳 200")
        void deactivateSuccess() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}", employeeUser.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            // 驗證已被停用
            User deactivated = userRepository.findById(employeeUser.getId()).orElseThrow();
            assertThat(deactivated.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("EMPLOYEE 停用使用者 — 回傳 403")
        void deactivateAsEmployee_forbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}", adminUser.getId())
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }
    }
}
