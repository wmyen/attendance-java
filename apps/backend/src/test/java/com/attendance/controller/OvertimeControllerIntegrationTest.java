package com.attendance.controller;

import com.attendance.IntegrationTest;
import com.attendance.entity.*;
import com.attendance.repository.*;
import com.attendance.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OvertimeController 整合測試。
 * 驗證加班申請、簽核等 API 端點與角色權限。
 */
@AutoConfigureMockMvc
class OvertimeControllerIntegrationTest extends IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OvertimeRequestRepository overtimeRequestRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User employee;
    private User manager;
    private String employeeToken;
    private String managerToken;
    private String adminToken;
    private User admin;

    @BeforeEach
    void setUp() {
        // 清理加班相關資料
        overtimeRequestRepository.deleteAll();

        Department dept = new Department();
        dept.setName("加班測試部");
        dept = departmentRepository.save(dept);

        admin = new User();
        admin.setEmail("ot-admin@test.com");
        admin.setName("加班管理員");
        admin.setRole(UserRole.ADMIN);
        admin.setDepartment(dept);
        admin.setPassword(passwordEncoder.encode("Admin@2026"));
        admin.setIsActive(true);
        admin.setMustChangePassword(false);
        admin = userRepository.save(admin);

        manager = new User();
        manager.setEmail("ot-manager@test.com");
        manager.setName("加班主管");
        manager.setRole(UserRole.MANAGER);
        manager.setDepartment(dept);
        manager.setManager(admin);
        manager.setPassword(passwordEncoder.encode("Manager@2026"));
        manager.setIsActive(true);
        manager.setMustChangePassword(false);
        manager = userRepository.save(manager);

        employee = new User();
        employee.setEmail("ot-emp@test.com");
        employee.setName("加班員工");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setDepartment(dept);
        employee.setManager(manager);
        employee.setPassword(passwordEncoder.encode("Emp@2026"));
        employee.setIsActive(true);
        employee.setMustChangePassword(false);
        employee = userRepository.save(employee);

        // 產生 Token
        employeeToken = jwtTokenProvider.generateAccessToken(
                employee.getId(), employee.getEmail(), employee.getRole().name());
        managerToken = jwtTokenProvider.generateAccessToken(
                manager.getId(), manager.getEmail(), manager.getRole().name());
        adminToken = jwtTokenProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole().name());
    }

    // ─── POST /api/v1/overtimes ───────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/overtimes")
    class ApplyTests {

        @Test
        @DisplayName("EMPLOYEE 加班申請 — 回傳 200 + OvertimeResponse")
        void apply_success() throws Exception {
            String body = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "專案趕工"
                    }
                    """;

            mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.reason").value("專案趕工"))
                    .andExpect(jsonPath("$.startTime").value("2026-06-20T18:00:00"))
                    .andExpect(jsonPath("$.endTime").value("2026-06-20T22:00:00"));
        }

        @Test
        @DisplayName("未帶 token — 回傳 403")
        void apply_noAuth() throws Exception {
            mockMvc.perform(post("/api/v1/overtimes")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── GET /api/v1/overtimes/my ─────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/overtimes/my")
    class MyOvertimesTests {

        @Test
        @DisplayName("查詢自己的加班記錄 — 回傳 200 + 列表")
        void myOvertimes_withData() throws Exception {
            // 先申請加班
            String body = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "加班"
                    }
                    """;

            mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());

            // 查詢我的加班
            mockMvc.perform(get("/api/v1/overtimes/my")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("無加班記錄 — 回傳 200 + 空列表")
        void myOvertimes_empty() throws Exception {
            mockMvc.perform(get("/api/v1/overtimes/my")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ─── GET /api/v1/overtimes/pending ────────────────────────

    @Nested
    @DisplayName("GET /api/v1/overtimes/pending")
    class PendingOvertimesTests {

        @Test
        @DisplayName("MANAGER 查詢待簽核加班 — 回傳 200")
        void pendingOvertimes_asManager() throws Exception {
            // 員工申請加班
            String body = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "專案趕工"
                    }
                    """;

            mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());

            // 主管查詢待簽核
            mockMvc.perform(get("/api/v1/overtimes/pending")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("EMPLOYEE 無法查詢待簽核 — 回傳 403")
        void pendingOvertimes_asEmployee() throws Exception {
            mockMvc.perform(get("/api/v1/overtimes/pending")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── PUT /api/v1/overtimes/{id}/approve ───────────────────

    @Nested
    @DisplayName("PUT /api/v1/overtimes/{id}/approve")
    class ApproveTests {

        private Long overtimeId;

        @BeforeEach
        void createOvertime() throws Exception {
            String body = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "專案趕工"
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            overtimeId = objectMapper.readTree(responseJson).get("id").asLong();
        }

        @Test
        @DisplayName("MANAGER 核准加班 — 回傳 200 + APPROVED")
        void approve_success() throws Exception {
            mockMvc.perform(put("/api/v1/overtimes/" + overtimeId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.approvedById").value(manager.getId()))
                    .andExpect(jsonPath("$.approvedByName").value("加班主管"))
                    .andExpect(jsonPath("$.approvedAt").isNotEmpty());
        }

        @Test
        @DisplayName("ADMIN 也能核准加班 — 回傳 200")
        void approve_byAdmin() throws Exception {
            mockMvc.perform(put("/api/v1/overtimes/" + overtimeId + "/approve")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        @Test
        @DisplayName("重複核准 — 回傳 400")
        void approve_duplicate() throws Exception {
            // 第一次核准
            mockMvc.perform(put("/api/v1/overtimes/" + overtimeId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 第二次核准 → 應失敗
            mockMvc.perform(put("/api/v1/overtimes/" + overtimeId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EMPLOYEE 無法核准 — 回傳 403")
        void approve_asEmployee() throws Exception {
            mockMvc.perform(put("/api/v1/overtimes/" + overtimeId + "/approve")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── PUT /api/v1/overtimes/{id}/reject ────────────────────

    @Nested
    @DisplayName("PUT /api/v1/overtimes/{id}/reject")
    class RejectTests {

        private Long overtimeId;

        @BeforeEach
        void createOvertime() throws Exception {
            String body = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "專案趕工"
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            overtimeId = objectMapper.readTree(responseJson).get("id").asLong();
        }

        @Test
        @DisplayName("MANAGER 駁回加班 — 回傳 200 + REJECTED")
        void reject_success() throws Exception {
            mockMvc.perform(put("/api/v1/overtimes/" + overtimeId + "/reject")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.approvedById").value(manager.getId()));
        }

        @Test
        @DisplayName("駁回後再核准 — 回傳 400")
        void reject_thenApprove() throws Exception {
            // 先駁回
            mockMvc.perform(put("/api/v1/overtimes/" + overtimeId + "/reject")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 再核准 → 應失敗
            mockMvc.perform(put("/api/v1/overtimes/" + overtimeId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── 完整流程（申請→簽核→查詢）─────────────────────────────

    @Nested
    @DisplayName("完整加班流程")
    class FullFlowTests {

        @Test
        @DisplayName("申請 → 主管核准 → 我的加班列表顯示 APPROVED")
        void fullFlow_approve() throws Exception {
            // 1. 申請加班
            String body = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "系統上線"
                    }
                    """;

            MvcResult applyResult = mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            Long otId = objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("id").asLong();

            // 2. 主管核准
            mockMvc.perform(put("/api/v1/overtimes/" + otId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 3. 查詢我的加班 → 狀態應為 APPROVED
            mockMvc.perform(get("/api/v1/overtimes/my")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("APPROVED"))
                    .andExpect(jsonPath("$[0].approvedById").value(manager.getId()));
        }

        @Test
        @DisplayName("申請 → 主管駁回 → 待簽核列表不再顯示")
        void fullFlow_reject() throws Exception {
            // 1. 申請加班
            String body = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "加班"
                    }
                    """;

            MvcResult applyResult = mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            Long otId = objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("id").asLong();

            // 2. 主管駁回
            mockMvc.perform(put("/api/v1/overtimes/" + otId + "/reject")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 3. 待簽核列表應為空
            mockMvc.perform(get("/api/v1/overtimes/pending")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
