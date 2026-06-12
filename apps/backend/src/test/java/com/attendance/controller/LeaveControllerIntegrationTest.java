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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LeaveController 整合測試。
 * 驗證請假申請、簽核、假別餘額查詢等 API 端點與角色權限。
 */
@AutoConfigureMockMvc
class LeaveControllerIntegrationTest extends IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private LeaveBalanceRepository leaveBalanceRepository;
    @Autowired private LeaveTypeRepository leaveTypeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User employee;
    private User manager;
    private User admin;
    private LeaveType annualLeave;
    private LeaveType sickLeave;
    private String employeeToken;
    private String managerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // 清理請假相關資料
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();

        Department dept = new Department();
        dept.setName("請假測試部");
        dept = departmentRepository.save(dept);

        admin = new User();
        admin.setEmail("leave-admin@test.com");
        admin.setName("請假管理員");
        admin.setRole(UserRole.ADMIN);
        admin.setDepartment(dept);
        admin.setPassword(passwordEncoder.encode("Admin@2026"));
        admin.setIsActive(true);
        admin.setMustChangePassword(false);
        admin = userRepository.save(admin);

        manager = new User();
        manager.setEmail("leave-manager@test.com");
        manager.setName("請假主管");
        manager.setRole(UserRole.MANAGER);
        manager.setDepartment(dept);
        manager.setManager(admin);
        manager.setPassword(passwordEncoder.encode("Manager@2026"));
        manager.setIsActive(true);
        manager.setMustChangePassword(false);
        manager = userRepository.save(manager);

        employee = new User();
        employee.setEmail("leave-emp@test.com");
        employee.setName("請假員工");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setDepartment(dept);
        employee.setManager(manager);
        employee.setPassword(passwordEncoder.encode("Emp@2026"));
        employee.setIsActive(true);
        employee.setMustChangePassword(false);
        employee = userRepository.save(employee);

        // 建立假別（H2 測試環境不跑 data.sql）
        annualLeave = new LeaveType(null, "特休", "ANNUAL", true, false);
        annualLeave = leaveTypeRepository.save(annualLeave);
        sickLeave = new LeaveType(null, "病假", "SICK", true, true);
        sickLeave = leaveTypeRepository.save(sickLeave);

        // 建立假別餘額
        createBalance(employee, annualLeave, new BigDecimal("7.0"));
        createBalance(employee, sickLeave, new BigDecimal("30.0"));
        createBalance(manager, annualLeave, new BigDecimal("14.0"));

        // 產生 Token
        employeeToken = jwtTokenProvider.generateAccessToken(
                employee.getId(), employee.getEmail(), employee.getRole().name());
        managerToken = jwtTokenProvider.generateAccessToken(
                manager.getId(), manager.getEmail(), manager.getRole().name());
        adminToken = jwtTokenProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole().name());
    }

    private void createBalance(User user, LeaveType leaveType, BigDecimal totalDays) {
        LeaveBalance balance = new LeaveBalance();
        balance.setUser(user);
        balance.setLeaveType(leaveType);
        balance.setYear(LocalDateTime.now().getYear());
        balance.setTotalDays(totalDays);
        balance.setUsedDays(BigDecimal.ZERO);
        leaveBalanceRepository.save(balance);
    }

    // ─── GET /api/v1/leaves/types ─────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/leaves/types")
    class ListTypesTests {

        @Test
        @DisplayName("取得假別列表 — 回傳 200 + 列表")
        void listTypes_success() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/types")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(not(empty()))));
        }

        @Test
        @DisplayName("未帶 token — 回傳 403")
        void listTypes_noAuth() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/types"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── POST /api/v1/leaves ──────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/leaves")
    class ApplyTests {

        @Test
        @DisplayName("EMPLOYEE 請假申請 — 回傳 200 + LeaveResponse")
        void apply_success() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "私人原因"
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.leaveTypeName").value("特休"))
                    .andExpect(jsonPath("$.reason").value("私人原因"));
        }

        @Test
        @DisplayName("請假含代理人 — 回傳 agentId 和 agentName")
        void apply_withAgent() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "家中有事",
                        "agentId": %d
                    }
                    """.formatted(sickLeave.getId(), manager.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agentId").value(manager.getId()))
                    .andExpect(jsonPath("$.agentName").value("請假主管"));
        }

        @Test
        @DisplayName("不存在的假別 — 回傳 404（ResourceNotFoundException）")
        void apply_invalidLeaveType() throws Exception {
            String body = """
                    {
                        "leaveTypeId": 99999,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "測試"
                    }
                    """;

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("未帶 token — 回傳 403")
        void apply_noAuth() throws Exception {
            mockMvc.perform(post("/api/v1/leaves")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── GET /api/v1/leaves/my ────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/leaves/my")
    class MyLeavesTests {

        @Test
        @DisplayName("查詢自己的請假記錄 — 回傳 200 + 列表")
        void myLeaves_withData() throws Exception {
            // 先請假
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "休假"
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());

            // 查詢我的請假
            mockMvc.perform(get("/api/v1/leaves/my")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("無請假記錄 — 回傳 200 + 空列表")
        void myLeaves_empty() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/my")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ─── GET /api/v1/leaves/pending ────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/leaves/pending")
    class PendingLeavesTests {

        @Test
        @DisplayName("MANAGER 查詢待簽核假單 — 回傳 200")
        void pendingLeaves_asManager() throws Exception {
            // 員工請假
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "休假"
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());

            // 主管查詢待簽核
            mockMvc.perform(get("/api/v1/leaves/pending")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("EMPLOYEE 無法查詢待簽核 — 回傳 403")
        void pendingLeaves_asEmployee() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/pending")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── PUT /api/v1/leaves/{id}/approve ──────────────────────

    @Nested
    @DisplayName("PUT /api/v1/leaves/{id}/approve")
    class ApproveTests {

        private Long leaveId;

        @BeforeEach
        void createLeave() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "休假"
                    }
                    """.formatted(annualLeave.getId());

            MvcResult result = mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            leaveId = objectMapper.readTree(responseJson).get("id").asLong();
        }

        @Test
        @DisplayName("MANAGER 核准假單 — 回傳 200 + APPROVED")
        void approve_success() throws Exception {
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.approvedById").value(manager.getId()))
                    .andExpect(jsonPath("$.approvedByName").value("請假主管"))
                    .andExpect(jsonPath("$.approvedAt").isNotEmpty());
        }

        @Test
        @DisplayName("核准後驗證餘額已扣減")
        void approve_balanceDeducted() throws Exception {
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 查詢餘額驗證
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.leaveTypeCode=='ANNUAL')].usedDays").value(hasItem(closeTo(1.0, 0.001))));
        }

        @Test
        @DisplayName("重複核准 — 回傳 400")
        void approve_duplicate() throws Exception {
            // 第一次核准
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 第二次核准 → 應失敗
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("EMPLOYEE 無法核准 — 回傳 403")
        void approve_asEmployee() throws Exception {
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── PUT /api/v1/leaves/{id}/reject ───────────────────────

    @Nested
    @DisplayName("PUT /api/v1/leaves/{id}/reject")
    class RejectTests {

        private Long leaveId;

        @BeforeEach
        void createLeave() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "休假"
                    }
                    """.formatted(annualLeave.getId());

            MvcResult result = mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            leaveId = objectMapper.readTree(responseJson).get("id").asLong();
        }

        @Test
        @DisplayName("MANAGER 駁回假單 — 回傳 200 + REJECTED")
        void reject_success() throws Exception {
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/reject")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.approvedById").value(manager.getId()));
        }

        @Test
        @DisplayName("駁回後餘額不變")
        void reject_balanceUnchanged() throws Exception {
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/reject")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 餘額應維持不變
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.leaveTypeCode=='ANNUAL')].usedDays").value(hasItem(is(0))));
        }
    }

    // ─── GET /api/v1/leaves/balance ────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/leaves/balance")
    class BalanceTests {

        @Test
        @DisplayName("查詢自己的假別餘額 — 回傳 200 + 餘額列表")
        void balance_success() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("ADMIN 查詢他人餘額 — 回傳 200")
        void balance_adminViewOther() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .param("userId", String.valueOf(employee.getId()))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("指定年度查詢 — 回傳對應年度餘額")
        void balance_withYear() throws Exception {
            int year = LocalDateTime.now().getYear();

            mockMvc.perform(get("/api/v1/leaves/balance")
                            .param("year", String.valueOf(year))
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("無餘額的年度 — 回傳空列表")
        void balance_emptyYear() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .param("year", "2020")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ─── 代理人制度 (Agent) ───────────────────────────────────

    @Nested
    @DisplayName("代理人制度（Agent）")
    class AgentTests {

        @Test
        @DisplayName("使用者有預設代理人 → 請假時自動填入代理人")
        void apply_usesDefaultAgent() throws Exception {
            // 設定員工的預設代理人為 admin
            employee.setAgent(admin);
            userRepository.save(employee);

            // 請假時不指定 agentId → 應自動使用預設代理人
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-25T09:00:00",
                        "endTime": "2026-06-25T17:00:00",
                        "reason": "休假"
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agentId").value(admin.getId()))
                    .andExpect(jsonPath("$.agentName").value("請假管理員"));
        }

        @Test
        @DisplayName("明確指定代理人 → 覆蓋預設代理人")
        void apply_explicitAgentOverridesDefault() throws Exception {
            // 設定員工的預設代理人為 admin
            employee.setAgent(admin);
            userRepository.save(employee);

            // 但請假時明確指定 manager 為代理人
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-25T09:00:00",
                        "endTime": "2026-06-25T17:00:00",
                        "reason": "休假",
                        "agentId": %d
                    }
                    """.formatted(annualLeave.getId(), manager.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agentId").value(manager.getId()))
                    .andExpect(jsonPath("$.agentName").value("請假主管"));
        }

        @Test
        @DisplayName("無預設代理人且未指定 → agentId 為 null")
        void apply_noDefaultNoExplicit() throws Exception {
            // employee 沒有預設代理人（setUp 中未設定 agent）
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-25T09:00:00",
                        "endTime": "2026-06-25T17:00:00",
                        "reason": "休假"
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agentId").isEmpty())
                    .andExpect(jsonPath("$.agentName").isEmpty());
        }

        @Test
        @DisplayName("待簽核假單 — 包含代理人資訊")
        void pendingLeaves_includeAgentInfo() throws Exception {
            // 設定預設代理人
            employee.setAgent(admin);
            userRepository.save(employee);

            // 請假（自動帶入代理人）
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-25T09:00:00",
                        "endTime": "2026-06-25T17:00:00",
                        "reason": "休假"
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());

            // 主管查詢待簽核 → 應包含代理人
            mockMvc.perform(get("/api/v1/leaves/pending")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].agentId").value(admin.getId()))
                    .andExpect(jsonPath("$[0].agentName").value("請假管理員"));
        }

        @Test
        @DisplayName("核准含代理人的假單 → 核准後仍保留代理人資訊")
        void approve_preservesAgentInfo() throws Exception {
            // 請假含代理人
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-25T09:00:00",
                        "endTime": "2026-06-25T17:00:00",
                        "reason": "休假",
                        "agentId": %d
                    }
                    """.formatted(annualLeave.getId(), manager.getId());

            MvcResult result = mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            Long leaveId = objectMapper.readTree(responseJson).get("id").asLong();

            // 核准
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.agentId").value(manager.getId()))
                    .andExpect(jsonPath("$.agentName").value("請假主管"));
        }

        @Test
        @DisplayName("代理人不存在 — 回傳 404")
        void apply_agentNotFound() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-25T09:00:00",
                        "endTime": "2026-06-25T17:00:00",
                        "reason": "測試",
                        "agentId": 99999
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }
}
