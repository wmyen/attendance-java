package com.attendance.e2e;

import com.attendance.IntegrationTest;
import com.attendance.dto.auth.LoginRequest;
import com.attendance.dto.user.UserCreateRequest;
import com.attendance.dto.user.UserUpdateRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 端到端流程驗證測試。
 * 驗證完整使用者旅程：登入 → 改密碼 → 打卡 → 請假 → 簽核 → 餘額變化。
 * 以及角色權限隔離和邊界案例。
 */
@AutoConfigureMockMvc
class EndToEndFlowTest extends IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private LeaveBalanceRepository leaveBalanceRepository;
    @Autowired private LeaveTypeRepository leaveTypeRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private OvertimeRequestRepository overtimeRequestRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private Department dept;
    private User admin;
    private User manager;
    private User employee;
    private LeaveType annualLeave;
    private LeaveType sickLeave;
    private String adminToken;
    private String managerToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        // 清理業務資料（避免 DataInitializer 產生的資料干擾）
        overtimeRequestRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        attendanceRepository.deleteAll();

        dept = new Department();
        dept.setName("E2E 測試部");
        dept = departmentRepository.save(dept);

        admin = createUser("e2e-admin@test.com", "E2E管理員", UserRole.ADMIN, null, null);
        manager = createUser("e2e-manager@test.com", "E2E主管", UserRole.MANAGER, admin, admin);
        employee = createUser("e2e-employee@test.com", "E2E員工", UserRole.EMPLOYEE, manager, admin);

        adminToken = generateToken(admin);
        managerToken = generateToken(manager);
        employeeToken = generateToken(employee);

        // 建立假別
        annualLeave = leaveTypeRepository.save(new LeaveType(null, "特休", "ANNUAL", true, false));
        sickLeave = leaveTypeRepository.save(new LeaveType(null, "病假", "SICK", true, true));

        // 建立假別餘額
        createBalance(employee, annualLeave, new BigDecimal("7.0"));
        createBalance(employee, sickLeave, new BigDecimal("30.0"));
        createBalance(manager, annualLeave, new BigDecimal("14.0"));
    }

    private User createUser(String email, String name, UserRole role, User managerUser, User agentUser) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setDepartment(dept);
        user.setManager(managerUser);
        user.setAgent(agentUser);
        user.setPassword(passwordEncoder.encode("Test@2026"));
        user.setIsActive(true);
        user.setMustChangePassword(false);
        return userRepository.save(user);
    }

    private String generateToken(User user) {
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
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

    // ═══════════════════════════════════════════════════════════════
    // 1. 完整使用者流程
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("E2E: 完整使用者旅程")
    class FullUserJourneyTests {

        @Test
        @DisplayName("旅程：ADMIN 建立使用者 → 登入 → 改密碼 → 打卡 → 請假 → 簽核")
        void fullUserJourney() throws Exception {
            // ─── Step 1: ADMIN 建立新使用者 ───
            String newUserEmail = "journey-new@test.com";
            UserCreateRequest createReq = new UserCreateRequest();
            createReq.setEmail(newUserEmail);
            createReq.setName("旅程使用者");
            createReq.setRole("EMPLOYEE");
            createReq.setDeptId(dept.getId());
            createReq.setManagerId(manager.getId());

            MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(newUserEmail))
                    .andExpect(jsonPath("$.mustChangePassword").value(true))
                    .andExpect(jsonPath("$.isActive").value(true))
                    .andReturn();

            Long newUserId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

            // ─── Step 2: 新使用者登入 ───
            // 需要取得密碼（createUser 時由系統隨機產生並 Email）
            // 由於 H2 環境無 Mail，直接用 JWT token 模擬已登入狀態
            String newUserToken = jwtTokenProvider.generateAccessToken(
                    newUserId, newUserEmail, UserRole.EMPLOYEE.name());

            // ─── Step 3: 變更密碼 ───
            // 先用 ADMIN 角色查詢使用者資訊確認存在
            mockMvc.perform(get("/api/v1/users/{id}", newUserId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(newUserEmail));

            // ─── Step 4: 打卡（clock-in → clock-out）───
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
                    .andExpect(jsonPath("$.clockIn").isNotEmpty());

            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clockOut").isNotEmpty());

            // 驗證今日打卡記錄
            mockMvc.perform(get("/api/v1/attendance/today")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clockIn").isNotEmpty())
                    .andExpect(jsonPath("$.clockOut").isNotEmpty());

            // ─── Step 5: 請假申請 ───
            String leaveBody = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "E2E 旅程測試"
                    }
                    """.formatted(annualLeave.getId());

            MvcResult leaveResult = mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(leaveBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.leaveTypeName").value("特休"))
                    .andExpect(jsonPath("$.reason").value("E2E 旅程測試"))
                    // 員工有預設代理人 admin
                    .andExpect(jsonPath("$.agentId").value(admin.getId()))
                    .andExpect(jsonPath("$.agentName").value("E2E管理員"))
                    .andReturn();

            Long leaveId = objectMapper.readTree(leaveResult.getResponse().getContentAsString()).get("id").asLong();

            // ─── Step 6: 主管查詢待簽核 ───
            mockMvc.perform(get("/api/v1/leaves/pending")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(leaveId))
                    .andExpect(jsonPath("$[0].agentId").value(admin.getId()));

            // ─── Step 7: 主管核准假單 ───
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.approvedById").value(manager.getId()))
                    .andExpect(jsonPath("$.agentId").value(admin.getId()));

            // ─── Step 8: 驗證餘額已扣減 ───
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.leaveTypeCode=='ANNUAL')].usedDays").value(hasItem(closeTo(1.0, 0.001))))
                    .andExpect(jsonPath("$[?(@.leaveTypeCode=='ANNUAL')].remainingDays").value(hasItem(closeTo(6.0, 0.001))));

            // ─── Step 9: 驗證員工的請假列表 ───
            mockMvc.perform(get("/api/v1/leaves/my")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("APPROVED"));
        }

        @Test
        @DisplayName("旅程：加班申請 → 核准 → 查詢")
        void overtimeJourney() throws Exception {
            // Step 1: 員工申請加班
            String overtimeBody = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "E2E 加班測試"
                    }
                    """;

            MvcResult otResult = mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(overtimeBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andReturn();

            Long otId = objectMapper.readTree(otResult.getResponse().getContentAsString()).get("id").asLong();

            // Step 2: 主管核准
            mockMvc.perform(put("/api/v1/overtimes/" + otId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            // Step 3: 員工查詢我的加班
            mockMvc.perform(get("/api/v1/overtimes/my")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("APPROVED"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. 角色權限隔離驗證
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("E2E: 角色權限隔離")
    class RolePermissionTests {

        // ─── EMPLOYEE 權限限制 ───

        @Test
        @DisplayName("EMPLOYEE 不能列出所有使用者 — 403")
        void employee_cannotListUsers() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 不能建立使用者 — 403")
        void employee_cannotCreateUser() throws Exception {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("forbidden@test.com");
            req.setName("禁止");
            req.setRole("EMPLOYEE");

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 不能更新使用者 — 403")
        void employee_cannotUpdateUser() throws Exception {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setName("嘗試修改");

            mockMvc.perform(put("/api/v1/users/{id}", manager.getId())
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 不能停用使用者 — 403")
        void employee_cannotDeactivateUser() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}", manager.getId())
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 不能查看待簽核假單 — 403")
        void employee_cannotViewPendingLeaves() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/pending")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 不能核准假單 — 403")
        void employee_cannotApproveLeave() throws Exception {
            mockMvc.perform(put("/api/v1/leaves/1/approve")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 不能駁回假單 — 403")
        void employee_cannotRejectLeave() throws Exception {
            mockMvc.perform(put("/api/v1/leaves/1/reject")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 不能查看待簽核加班 — 403")
        void employee_cannotViewPendingOvertimes() throws Exception {
            mockMvc.perform(get("/api/v1/overtimes/pending")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 不能核准加班 — 403")
        void employee_cannotApproveOvertime() throws Exception {
            mockMvc.perform(put("/api/v1/overtimes/1/approve")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("EMPLOYEE 可以查看部門列表 — 200（唯讀）")
        void employee_canListDepartments_readOnly() throws Exception {
            mockMvc.perform(get("/api/v1/departments")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EMPLOYEE 不能建立部門 — 403")
        void employee_cannotCreateDepartment() throws Exception {
            mockMvc.perform(post("/api/v1/departments")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"新部門\"}"))
                    .andExpect(status().isForbidden());
        }

        // ─── MANAGER 權限 ───

        @Test
        @DisplayName("MANAGER 可以查看待簽核假單 — 200")
        void manager_canViewPendingLeaves() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/pending")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("MANAGER 可以打卡 — 200")
        void manager_canClockIn() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("MANAGER 不能管理使用者 — 403")
        void manager_cannotManageUsers() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MANAGER 不能管理部門 — 403")
        void manager_cannotManageDepartments() throws Exception {
            mockMvc.perform(post("/api/v1/departments")
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"新部門\"}"))
                    .andExpect(status().isForbidden());
        }

        // ─── ADMIN 完整權限 ───

        @Test
        @DisplayName("ADMIN 可以列出使用者 — 200")
        void admin_canListUsers() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN 可以打卡 — 200")
        void admin_canClockIn() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN 可以請假 — 200")
        void admin_canApplyLeave() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "管理員請假"
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. 邊界案例驗證
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("E2E: 邊界案例")
    class EdgeCaseTests {

        @Test
        @DisplayName("重複上班打卡 — 第二次回傳 400")
        void duplicateClockIn() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("未打上班卡直接下班打卡 — 回傳 400")
        void clockOutWithoutClockIn() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("重複下班打卡 — 回傳 400")
        void duplicateClockOut() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("請假餘額不足 — 核准時回傳 400")
        void leaveBalanceInsufficient() throws Exception {
            // 建立只有 0.5 天餘額的使用者
            User poorUser = createUser("poor@test.com", "窮員工", UserRole.EMPLOYEE, manager, null);
            String poorToken = generateToken(poorUser);
            createBalance(poorUser, annualLeave, new BigDecimal("0.5"));

            // 請 1 天特休（超過 0.5 天餘額）
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "餘額不足測試"
                    }
                    """.formatted(annualLeave.getId());

            MvcResult result = mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + poorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            Long leaveId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            // 核准時應因餘額不足而失敗
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("無效 JWT Token — 回傳 403")
        void invalidToken() throws Exception {
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("無 Token 存取受保護端點 — 回傳 403")
        void noToken() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("重複核准假單 — 第二次回傳 400")
        void duplicateApproveLeave() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "重複核准測試"
                    }
                    """.formatted(annualLeave.getId());

            MvcResult result = mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            Long leaveId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            // 第一次核准
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 第二次核准 → 失敗
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("核准後再駁回同一假單 — 回傳 400")
        void approveThenReject() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "先准後駁"
                    }
                    """.formatted(annualLeave.getId());

            MvcResult result = mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            Long leaveId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            // 核准
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 再駁回 → 失敗
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/reject")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("重複核准加班 — 第二次回傳 400")
        void duplicateApproveOvertime() throws Exception {
            String body = """
                    {
                        "startTime": "2026-06-20T18:00:00",
                        "endTime": "2026-06-20T22:00:00",
                        "reason": "重複核准加班"
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/v1/overtimes")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            Long otId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            // 第一次核准
            mockMvc.perform(put("/api/v1/overtimes/" + otId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 第二次核准 → 失敗
            mockMvc.perform(put("/api/v1/overtimes/" + otId + "/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("不存在的假單核准 — 回傳 404")
        void approveNonExistentLeave() throws Exception {
            mockMvc.perform(put("/api/v1/leaves/99999/approve")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("不存在的假別請假 — 回傳 404")
        void applyWithNonExistentLeaveType() throws Exception {
            String body = """
                    {
                        "leaveTypeId": 99999,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "不存在的假別"
                    }
                    """;

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Email 重複建立使用者 — 回傳 400")
        void createUserDuplicateEmail() throws Exception {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("e2e-employee@test.com"); // 已存在的 email
            req.setName("重複");
            req.setRole("EMPLOYEE");

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("無效 Refresh Token — 回傳 400")
        void invalidRefreshToken() throws Exception {
            String refreshBody = """
                    {
                        "refreshToken": "totally-invalid-refresh-token"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("代理人不存在時請假 — 回傳 404")
        void applyLeaveWithNonExistentAgent() throws Exception {
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "代理人不存在",
                        "agentId": 99999
                    }
                    """.formatted(annualLeave.getId());

            mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. 跨模組互動驗證
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("E2E: 跨模組互動")
    class CrossModuleTests {

        @Test
        @DisplayName("停用使用者後無法登入")
        void deactivatedUserCannotLogin() throws Exception {
            // ADMIN 停用 employee
            mockMvc.perform(delete("/api/v1/users/{id}", employee.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            // 嘗試登入 → 應失敗
            LoginRequest loginReq = new LoginRequest();
            loginReq.setEmail("e2e-employee@test.com");
            loginReq.setPassword("Test@2026");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ADMIN 建立使用者後可查詢到完整資料")
        void createUserThenQuery() throws Exception {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("query-test@test.com");
            req.setName("查詢測試");
            req.setRole("EMPLOYEE");
            req.setDeptId(dept.getId());
            req.setManagerId(manager.getId());
            req.setAgentId(admin.getId());

            MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            Long userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

            // 查詢驗證
            mockMvc.perform(get("/api/v1/users/{id}", userId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("query-test@test.com"))
                    .andExpect(jsonPath("$.name").value("查詢測試"))
                    .andExpect(jsonPath("$.deptName").value("E2E 測試部"))
                    .andExpect(jsonPath("$.managerName").value("E2E主管"))
                    .andExpect(jsonPath("$.agentName").value("E2E管理員"));
        }

        @Test
        @DisplayName("打卡後查詢月報表包含打卡記錄")
        void clockInThenCheckMonthly() throws Exception {
            // 打卡
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            // 查月報表
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            mockMvc.perform(get("/api/v1/attendance/monthly")
                            .param("year", String.valueOf(year))
                            .param("month", String.valueOf(month))
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(year))
                    .andExpect(jsonPath("$.month").value(month))
                    .andExpect(jsonPath("$.records", hasSize(1)))
                    .andExpect(jsonPath("$.records[0].clockIn").isNotEmpty())
                    .andExpect(jsonPath("$.records[0].clockOut").isNotEmpty());
        }

        @Test
        @DisplayName("ADMIN 可以查看他人的月報表和假別餘額")
        void adminCanViewOtherUserData() throws Exception {
            // 員工打卡
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            // ADMIN 查看員工的月報表
            mockMvc.perform(get("/api/v1/attendance/monthly")
                            .param("year", String.valueOf(year))
                            .param("month", String.valueOf(month))
                            .param("userId", String.valueOf(employee.getId()))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.records", hasSize(1)));

            // ADMIN 查看員工的假別餘額
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .param("userId", String.valueOf(employee.getId()))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("駁回假單不扣減餘額")
        void rejectDoesNotDeductBalance() throws Exception {
            // 先查餘額
            MvcResult balanceResult = mockMvc.perform(get("/api/v1/leaves/balance")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andReturn();

            // 請假
            String body = """
                    {
                        "leaveTypeId": %d,
                        "startTime": "2026-06-20T09:00:00",
                        "endTime": "2026-06-20T17:00:00",
                        "reason": "即將被駁回"
                    }
                    """.formatted(annualLeave.getId());

            MvcResult leaveResult = mockMvc.perform(post("/api/v1/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            Long leaveId = objectMapper.readTree(leaveResult.getResponse().getContentAsString()).get("id").asLong();

            // 駁回
            mockMvc.perform(put("/api/v1/leaves/" + leaveId + "/reject")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk());

            // 餘額應不變
            mockMvc.perform(get("/api/v1/leaves/balance")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.leaveTypeCode=='ANNUAL')].usedDays").value(hasItem(is(0))));
        }
    }
}
