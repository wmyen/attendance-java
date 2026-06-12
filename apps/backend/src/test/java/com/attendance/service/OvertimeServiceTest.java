package com.attendance.service;

import com.attendance.dto.overtime.OvertimeApplyRequest;
import com.attendance.dto.overtime.OvertimeResponse;
import com.attendance.entity.*;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.LeaveBalanceRepository;
import com.attendance.repository.LeaveTypeRepository;
import com.attendance.repository.OvertimeRequestRepository;
import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OvertimeService 單元測試。
 * 驗證加班申請、核准、駁回等商業邏輯。
 */
class OvertimeServiceTest extends ServiceTestBase {

    @Mock private OvertimeRequestRepository overtimeRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private MailService mailService;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;

    @InjectMocks private OvertimeService overtimeService;

    private User employee;
    private User manager;

    @BeforeEach
    void setUp() {
        Department dept = new Department(1L, "資訊部", null);

        manager = new User();
        manager.setId(10L);
        manager.setEmail("manager@test.com");
        manager.setName("王主管");
        manager.setRole(UserRole.MANAGER);
        manager.setDepartment(dept);
        manager.setIsActive(true);

        employee = new User();
        employee.setId(1L);
        employee.setEmail("emp@test.com");
        employee.setName("李員工");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setDepartment(dept);
        employee.setManager(manager);
        employee.setIsActive(true);
    }

    // ─── apply ───────────────────────────────────────────────

    @Nested
    @DisplayName("apply()")
    class ApplyTests {

        @Test
        @DisplayName("正常加班申請 — 狀態為 PENDING")
        void apply_success() {
            OvertimeApplyRequest request = new OvertimeApplyRequest();
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 22, 0));
            request.setReason("專案趕工");

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> {
                OvertimeRequest or = inv.getArgument(0);
                or.setId(300L);
                return or;
            });

            OvertimeResponse response = overtimeService.apply(1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getReason()).isEqualTo("專案趕工");
            assertThat(response.getStartTime()).isEqualTo(LocalDateTime.of(2026, 6, 15, 18, 0));
            assertThat(response.getEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 15, 22, 0));

            verify(mailService).sendOvertimeApplicationNotification(
                    eq("manager@test.com"), eq("李員工"), anyString(), anyString());
        }

        @Test
        @DisplayName("使用者不存在 — 拋出 ResourceNotFoundException")
        void apply_userNotFound() {
            OvertimeApplyRequest request = new OvertimeApplyRequest();
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 22, 0));
            request.setReason("測試");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> overtimeService.apply(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("使用者不存在");
        }

        @Test
        @DisplayName("員工沒有主管 — 不寄 Email 通知")
        void apply_noManager() {
            employee.setManager(null);

            OvertimeApplyRequest request = new OvertimeApplyRequest();
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 20, 0));
            request.setReason("加班");

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> {
                OvertimeRequest or = inv.getArgument(0);
                or.setId(301L);
                return or;
            });

            overtimeService.apply(1L, request);

            verify(mailService, never()).sendOvertimeApplicationNotification(
                    anyString(), anyString(), anyString(), anyString());
        }
    }

    // ─── approve ─────────────────────────────────────────────

    @Nested
    @DisplayName("approve()")
    class ApproveTests {

        private OvertimeRequest pendingOvertime;

        @BeforeEach
        void setUpOvertime() {
            pendingOvertime = new OvertimeRequest();
            pendingOvertime.setId(300L);
            pendingOvertime.setUser(employee);
            pendingOvertime.setStartTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            pendingOvertime.setEndTime(LocalDateTime.of(2026, 6, 15, 22, 0));
            pendingOvertime.setReason("專案趕工");
            pendingOvertime.setStatus(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("核准加班 — 狀態變 APPROVED")
        void approve_success() {
            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            OvertimeResponse response = overtimeService.approve(300L, 10L);

            assertThat(response.getStatus()).isEqualTo("APPROVED");
            assertThat(response.getApprovedById()).isEqualTo(10L);
            assertThat(response.getApprovedByName()).isEqualTo("王主管");
            assertThat(response.getApprovedAt()).isNotNull();

            verify(mailService).sendOvertimeApprovalResult(eq("emp@test.com"), eq(true),
                    eq("2026-06-15T18:00"), eq("2026-06-15T22:00"));
        }

        @Test
        @DisplayName("重複核准 — 拋出「此加班申請已簽核」")
        void approve_alreadyApproved() {
            pendingOvertime.setStatus(RequestStatus.APPROVED);

            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));

            assertThatThrownBy(() -> overtimeService.approve(300L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("此加班申請已簽核");

            verify(overtimeRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("核准已被駁回的加班 — 拋出「此加班申請已簽核」")
        void approve_alreadyRejected() {
            pendingOvertime.setStatus(RequestStatus.REJECTED);

            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));

            assertThatThrownBy(() -> overtimeService.approve(300L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("此加班申請已簽核");
        }

        @Test
        @DisplayName("加班申請不存在 — 拋出 ResourceNotFoundException")
        void approve_notFound() {
            when(overtimeRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> overtimeService.approve(999L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("加班申請不存在");
        }
    }

    // ─── reject ──────────────────────────────────────────────

    @Nested
    @DisplayName("reject()")
    class RejectTests {

        private OvertimeRequest pendingOvertime;

        @BeforeEach
        void setUpOvertime() {
            pendingOvertime = new OvertimeRequest();
            pendingOvertime.setId(300L);
            pendingOvertime.setUser(employee);
            pendingOvertime.setStartTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            pendingOvertime.setEndTime(LocalDateTime.of(2026, 6, 15, 22, 0));
            pendingOvertime.setReason("專案趕工");
            pendingOvertime.setStatus(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("駁回加班 — 狀態變 REJECTED")
        void reject_success() {
            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            OvertimeResponse response = overtimeService.reject(300L, 10L);

            assertThat(response.getStatus()).isEqualTo("REJECTED");
            assertThat(response.getApprovedById()).isEqualTo(10L);
            assertThat(response.getApprovedByName()).isEqualTo("王主管");
            assertThat(response.getApprovedAt()).isNotNull();

            verify(mailService).sendOvertimeApprovalResult(eq("emp@test.com"), eq(false),
                    eq("2026-06-15T18:00"), eq("2026-06-15T22:00"));
        }

        @Test
        @DisplayName("重複駁回 — 拋出「此加班申請已簽核」")
        void reject_alreadyProcessed() {
            pendingOvertime.setStatus(RequestStatus.REJECTED);

            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));

            assertThatThrownBy(() -> overtimeService.reject(300L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("此加班申請已簽核");

            verify(overtimeRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("加班申請不存在 — 拋出 ResourceNotFoundException")
        void reject_notFound() {
            when(overtimeRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> overtimeService.reject(999L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("加班申請不存在");
        }
    }

    // ─── getMyOvertimes ──────────────────────────────────────

    @Nested
    @DisplayName("getMyOvertimes()")
    class GetMyOvertimesTests {

        @Test
        @DisplayName("查詢自己的加班記錄 — 回傳列表")
        void getMyOvertimes_success() {
            OvertimeRequest or1 = new OvertimeRequest();
            or1.setId(300L);
            or1.setUser(employee);
            or1.setStartTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            or1.setEndTime(LocalDateTime.of(2026, 6, 15, 22, 0));
            or1.setReason("趕工");
            or1.setStatus(RequestStatus.PENDING);

            when(overtimeRequestRepository.findByUserId(1L)).thenReturn(List.of(or1));

            List<OvertimeResponse> responses = overtimeService.getMyOvertimes(1L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getReason()).isEqualTo("趕工");
            assertThat(responses.get(0).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("無加班記錄 — 回傳空列表")
        void getMyOvertimes_empty() {
            when(overtimeRequestRepository.findByUserId(1L)).thenReturn(List.of());

            List<OvertimeResponse> responses = overtimeService.getMyOvertimes(1L);

            assertThat(responses).isEmpty();
        }
    }

    // ─── getPendingOvertimes ─────────────────────────────────

    @Nested
    @DisplayName("getPendingOvertimes()")
    class GetPendingOvertimesTests {

        @Test
        @DisplayName("主管查詢待簽核加班 — 回傳列表")
        void getPendingOvertimes_success() {
            OvertimeRequest or1 = new OvertimeRequest();
            or1.setId(300L);
            or1.setUser(employee);
            or1.setStartTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            or1.setEndTime(LocalDateTime.of(2026, 6, 15, 22, 0));
            or1.setReason("專案趕工");
            or1.setStatus(RequestStatus.PENDING);

            when(overtimeRequestRepository.findByStatusAndUser_ManagerId(RequestStatus.PENDING, 10L))
                    .thenReturn(List.of(or1));

            List<OvertimeResponse> responses = overtimeService.getPendingOvertimes(10L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("無待簽核加班 — 回傳空列表")
        void getPendingOvertimes_empty() {
            when(overtimeRequestRepository.findByStatusAndUser_ManagerId(RequestStatus.PENDING, 10L))
                    .thenReturn(List.of());

            List<OvertimeResponse> responses = overtimeService.getPendingOvertimes(10L);

            assertThat(responses).isEmpty();
        }
    }

    // ─── 補休自動產生 ──────────────────────────────────────────

    @Nested
    @DisplayName("補休自動產生（approve 時觸發）")
    class CompensatoryLeaveTests {

        private OvertimeRequest pendingOvertime;
        private LeaveType compensatoryType;

        @BeforeEach
        void setUpOvertime() {
            compensatoryType = new LeaveType(200L, "補休", "COMPENSATORY", true, false);

            pendingOvertime = new OvertimeRequest();
            pendingOvertime.setId(300L);
            pendingOvertime.setUser(employee);
            pendingOvertime.setStartTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            pendingOvertime.setEndTime(LocalDateTime.of(2026, 6, 15, 22, 0));
            pendingOvertime.setReason("專案趕工");
            pendingOvertime.setStatus(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("核准加班 4 小時 — 自動產生 0.5 天補休餘額（新建）")
        void approve_generatesCompensatory_newBalance() {
            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveTypeRepository.findByCode("COMPENSATORY")).thenReturn(Optional.of(compensatoryType));
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 200L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            overtimeService.approve(300L, 10L);

            // 4 小時 / 8 = 0.5 天
            verify(leaveBalanceRepository).save(argThat(b ->
                    b.getTotalDays().compareTo(new BigDecimal("0.5")) == 0 &&
                    b.getUsedDays().compareTo(BigDecimal.ZERO) == 0 &&
                    b.getYear() == 2026
            ));
        }

        @Test
        @DisplayName("核准加班 8 小時 — 補休 1.0 天")
        void approve_compensatory_oneDay() {
            pendingOvertime.setEndTime(LocalDateTime.of(2026, 6, 16, 2, 0)); // 8 小時

            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveTypeRepository.findByCode("COMPENSATORY")).thenReturn(Optional.of(compensatoryType));
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 200L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            overtimeService.approve(300L, 10L);

            verify(leaveBalanceRepository).save(argThat(b ->
                    b.getTotalDays().compareTo(new BigDecimal("1.0")) == 0
            ));
        }

        @Test
        @DisplayName("已有補休餘額 — 累加而非新建")
        void approve_compensatory_accumulate() {
            LeaveBalance existingBalance = new LeaveBalance();
            existingBalance.setId(50L);
            existingBalance.setUser(employee);
            existingBalance.setLeaveType(compensatoryType);
            existingBalance.setYear(2026);
            existingBalance.setTotalDays(new BigDecimal("1.0"));
            existingBalance.setUsedDays(BigDecimal.ZERO);

            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveTypeRepository.findByCode("COMPENSATORY")).thenReturn(Optional.of(compensatoryType));
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 200L, 2026))
                    .thenReturn(Optional.of(existingBalance));
            when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            overtimeService.approve(300L, 10L);

            // 1.0 + 0.5 = 1.5
            verify(leaveBalanceRepository).save(argThat(b ->
                    b.getTotalDays().compareTo(new BigDecimal("1.5")) == 0
            ));
        }

        @Test
        @DisplayName("無 COMPENSATORY 假別 — 不產生補休，核准仍成功")
        void approve_noCompensatoryType() {
            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveTypeRepository.findByCode("COMPENSATORY")).thenReturn(Optional.empty());
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            OvertimeResponse response = overtimeService.approve(300L, 10L);

            assertThat(response.getStatus()).isEqualTo("APPROVED");
            verify(leaveBalanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("駁回加班 — 不產生補休")
        void reject_noCompensatory() {
            when(overtimeRequestRepository.findById(300L)).thenReturn(Optional.of(pendingOvertime));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(overtimeRequestRepository.save(any(OvertimeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            overtimeService.reject(300L, 10L);

            verify(leaveBalanceRepository, never()).save(any());
            verify(leaveTypeRepository, never()).findByCode(anyString());
        }
    }
}
