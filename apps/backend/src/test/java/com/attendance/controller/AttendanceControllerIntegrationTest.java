package com.attendance.controller;

import com.attendance.entity.Attendance;
import com.attendance.entity.AttendanceStatus;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.entity.UserRole;
import com.attendance.repository.AttendanceRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AttendanceController 整合測試。
 * 驗證 clock-in / clock-out / today / monthly 端點與角色權限。
 */
@AutoConfigureMockMvc
class AttendanceControllerIntegrationTest extends com.attendance.IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User employeeUser;
    private User adminUser;
    private String employeeToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // 清理 attendance 記錄（避免 DataInitializer 產生的資料干擾）
        attendanceRepository.deleteAll();

        Department dept = new Department();
        dept.setName("打卡測試部");
        dept = departmentRepository.save(dept);

        employeeUser = new User();
        employeeUser.setEmail("att-emp@test.com");
        employeeUser.setName("打卡員工");
        employeeUser.setRole(UserRole.EMPLOYEE);
        employeeUser.setDepartment(dept);
        employeeUser.setPassword(passwordEncoder.encode("Emp@2026"));
        employeeUser.setIsActive(true);
        employeeUser.setMustChangePassword(false);
        employeeUser = userRepository.save(employeeUser);

        adminUser = new User();
        adminUser.setEmail("att-admin@test.com");
        adminUser.setName("打卡管理員");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setDepartment(dept);
        adminUser.setPassword(passwordEncoder.encode("Admin@2026"));
        adminUser.setIsActive(true);
        adminUser.setMustChangePassword(false);
        adminUser = userRepository.save(adminUser);

        employeeToken = jwtTokenProvider.generateAccessToken(
                employeeUser.getId(), employeeUser.getEmail(), employeeUser.getRole().name());
        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
    }

    // ─── POST /api/v1/attendance/clock-in ─────────────────────

    @Nested
    @DisplayName("POST /api/v1/attendance/clock-in")
    class ClockInTests {

        @Test
        @DisplayName("EMPLOYEE 上班打卡 — 回傳 200 + ClockResponse")
        void clockIn_success() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
                    .andExpect(jsonPath("$.clockIn").isNotEmpty())
                    .andExpect(jsonPath("$.status").isNotEmpty());
        }

        @Test
        @DisplayName("重複上班打卡 — 回傳 400")
        void clockIn_duplicate() throws Exception {
            // 第一次打卡
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            // 第二次打卡 → 應失敗
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("未帶 token — 回傳 403")
        void clockIn_noAuth() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── POST /api/v1/attendance/clock-out ─────────────────────

    @Nested
    @DisplayName("POST /api/v1/attendance/clock-out")
    class ClockOutTests {

        @Test
        @DisplayName("上班打卡後下班打卡 — 回傳 200 + 含 clockOut")
        void clockOut_afterClockIn() throws Exception {
            // 先上班打卡
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            // 下班打卡
            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clockOut").isNotEmpty());
        }

        @Test
        @DisplayName("未打上班卡直接下班 — 回傳 400")
        void clockOut_withoutClockIn() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("重複下班打卡 — 回傳 400")
        void clockOut_duplicate() throws Exception {
            // 上班 + 下班
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            // 重複下班
            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── GET /api/v1/attendance/today ──────────────────────────

    @Nested
    @DisplayName("GET /api/v1/attendance/today")
    class TodayTests {

        @Test
        @DisplayName("已打卡 — 回傳 200 + ClockResponse")
        void today_found() throws Exception {
            // 先打卡
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            // 查詢今日
            mockMvc.perform(get("/api/v1/attendance/today")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clockIn").isNotEmpty())
                    .andExpect(jsonPath("$.date").value(LocalDate.now().toString()));
        }

        @Test
        @DisplayName("尚未打卡 — 回傳 204 No Content")
        void today_notFound() throws Exception {
            mockMvc.perform(get("/api/v1/attendance/today")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isNoContent());
        }
    }

    // ─── GET /api/v1/attendance/monthly ────────────────────────

    @Nested
    @DisplayName("GET /api/v1/attendance/monthly")
    class MonthlyTests {

        @Test
        @DisplayName("查詢當月紀錄 — 回傳 200 + MonthlyResponse")
        void monthly_current() throws Exception {
            // 先建立一筆打卡記錄
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            mockMvc.perform(get("/api/v1/attendance/monthly")
                            .param("year", String.valueOf(year))
                            .param("month", String.valueOf(month))
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(year))
                    .andExpect(jsonPath("$.month").value(month))
                    .andExpect(jsonPath("$.records").isArray())
                    .andExpect(jsonPath("$.records.length()").value(1));
        }

        @Test
        @DisplayName("查無紀錄的月份 — 回傳 200 + 空列表")
        void monthly_empty() throws Exception {
            mockMvc.perform(get("/api/v1/attendance/monthly")
                            .param("year", "2025")
                            .param("month", "1")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.records").isArray())
                    .andExpect(jsonPath("$.records.length()").value(0));
        }

        @Test
        @DisplayName("ADMIN 查詢他人月紀錄 — 回傳 200")
        void monthly_adminViewOther() throws Exception {
            // 員工打卡
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            // ADMIN 查看員工的月紀錄
            mockMvc.perform(get("/api/v1/attendance/monthly")
                            .param("year", String.valueOf(year))
                            .param("month", String.valueOf(month))
                            .param("userId", String.valueOf(employeeUser.getId()))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.records.length()").value(1));
        }

        @Test
        @DisplayName("預設參數（不帶 year/month）— 回傳當月")
        void monthly_defaultParams() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/attendance/monthly")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(LocalDate.now().getYear()))
                    .andExpect(jsonPath("$.month").value(LocalDate.now().getMonthValue()));
        }
    }

    // ─── 遲到/早退狀態驗證 ─────────────────────────────────────

    @Nested
    @DisplayName("遲到/早退狀態驗證")
    class StatusLogicTests {

        @Test
        @DisplayName("上班打卡後驗證資料庫記錄狀態正確")
        void verifyStatusInDb() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk())
                    .andReturn();

            // 從資料庫驗證
            Attendance saved = attendanceRepository
                    .findByUserIdAndDate(employeeUser.getId(), LocalDate.now())
                    .orElseThrow();

            assertThat(saved.getClockIn()).isNotNull();
            // 狀態必須是 NORMAL 或 LATE
            assertThat(saved.getStatus()).isIn(AttendanceStatus.NORMAL, AttendanceStatus.LATE);
        }

        @Test
        @DisplayName("上下班都打卡後驗證 clockIn/clockOut 皆有值")
        void verifyCompleteRecord() throws Exception {
            mockMvc.perform(post("/api/v1/attendance/clock-in")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/attendance/clock-out")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());

            Attendance saved = attendanceRepository
                    .findByUserIdAndDate(employeeUser.getId(), LocalDate.now())
                    .orElseThrow();

            assertThat(saved.getClockIn()).isNotNull();
            assertThat(saved.getClockOut()).isNotNull();
            assertThat(saved.getStatus()).isNotNull();
        }
    }
}
