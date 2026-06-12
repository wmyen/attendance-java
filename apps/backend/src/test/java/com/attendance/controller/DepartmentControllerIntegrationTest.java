package com.attendance.controller;

import com.attendance.dto.department.DepartmentRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DepartmentController 整合測試。
 * 驗證部門 CRUD + 權限隔離（list 公開，create/update 限 ADMIN）。
 */
@AutoConfigureMockMvc
class DepartmentControllerIntegrationTest extends com.attendance.IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User employeeUser;
    private String adminToken;
    private String employeeToken;
    private Department existingDept;

    @BeforeEach
    void setUp() {
        existingDept = new Department();
        existingDept.setName("測試部");
        existingDept = departmentRepository.save(existingDept);

        Department dept2 = new Department();
        dept2.setName("另一個部門");
        departmentRepository.save(dept2);

        // ADMIN
        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setName("管理員");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setPassword(passwordEncoder.encode("Admin@2026"));
        adminUser.setIsActive(true);
        adminUser.setMustChangePassword(false);
        adminUser = userRepository.save(adminUser);

        // EMPLOYEE
        employeeUser = new User();
        employeeUser.setEmail("employee@test.com");
        employeeUser.setName("員工");
        employeeUser.setRole(UserRole.EMPLOYEE);
        employeeUser.setPassword(passwordEncoder.encode("Emp@2026"));
        employeeUser.setIsActive(true);
        employeeUser.setMustChangePassword(false);
        employeeUser = userRepository.save(employeeUser);

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
        employeeToken = jwtTokenProvider.generateAccessToken(
                employeeUser.getId(), employeeUser.getEmail(), employeeUser.getRole().name());
    }

    // ─── GET /api/v1/departments ────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/departments")
    class ListDepartmentsTests {

        @Test
        @DisplayName("ADMIN 列出所有部門 — 回傳 200")
        void listAsAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/departments")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").isNotEmpty());
        }

        @Test
        @DisplayName("EMPLOYEE 也可列出部門 — 回傳 200（列表為公開）")
        void listAsEmployee() throws Exception {
            mockMvc.perform(get("/api/v1/departments")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    // ─── POST /api/v1/departments ────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/departments")
    class CreateDepartmentTests {

        @Test
        @DisplayName("ADMIN 建立部門 — 回傳 200")
        void createSuccess() throws Exception {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("研發部");

            mockMvc.perform(post("/api/v1/departments")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("研發部"))
                    .andExpect(jsonPath("$.id").isNumber());
        }

        @Test
        @DisplayName("EMPLOYEE 建立部門 — 回傳 403")
        void createAsEmployee_forbidden() throws Exception {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("不允許的部門");

            mockMvc.perform(post("/api/v1/departments")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("名稱為空 — 回傳 400 驗證錯誤")
        void createEmptyName() throws Exception {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("");

            mockMvc.perform(post("/api/v1/departments")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("部門名稱重複 — 回傳 400")
        void createFail_duplicateName() throws Exception {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("測試部"); // 已存在於 setUp

            mockMvc.perform(post("/api/v1/departments")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("部門名稱已存在"));
        }
    }

    // ─── PUT /api/v1/departments/{id} ────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/departments/{id}")
    class UpdateDepartmentTests {

        @Test
        @DisplayName("ADMIN 更新部門名稱 — 回傳 200")
        void updateSuccess() throws Exception {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("更新部門名");

            mockMvc.perform(put("/api/v1/departments/{id}", existingDept.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("更新部門名"));
        }

        @Test
        @DisplayName("EMPLOYEE 更新部門 — 回傳 403")
        void updateAsEmployee_forbidden() throws Exception {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("嘗試更新");

            mockMvc.perform(put("/api/v1/departments/{id}", existingDept.getId())
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("部門不存在 — 回傳 400")
        void updateNotFound() throws Exception {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("不存在的部門");

            mockMvc.perform(put("/api/v1/departments/{id}", 9999L)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }
}
