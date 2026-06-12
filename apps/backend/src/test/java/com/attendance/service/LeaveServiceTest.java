package com.attendance.service;

import com.attendance.dto.leave.LeaveApplyRequest;
import com.attendance.dto.leave.LeaveBalanceResponse;
import com.attendance.dto.leave.LeaveResponse;
import com.attendance.entity.*;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.LeaveBalanceRepository;
import com.attendance.repository.LeaveRequestRepository;
import com.attendance.repository.LeaveTypeRepository;
import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * LeaveService 單元測試。
 * 驗證請假申請、簽核（核准/駁回）、假別餘額查詢等商業邏輯。
 */
class LeaveServiceTest extends ServiceTestBase {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MailService mailService;

    @InjectMocks private LeaveService leaveService;

    private User employee;
    private User manager;
    private User agent;
    private LeaveType annualLeave;
    private LeaveType sickLeave;
    private LeaveBalance annualBalance;

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

        agent = new User();
        agent.setId(2L);
        agent.setEmail("agent@test.com");
        agent.setName("張代理人");
        agent.setRole(UserRole.EMPLOYEE);
        agent.setDepartment(dept);
        agent.setIsActive(true);

        annualLeave = new LeaveType(100L, "特休", "ANNUAL", true, false);
        sickLeave = new LeaveType(101L, "病假", "SICK", true, true);

        annualBalance = new LeaveBalance();
        annualBalance.setId(1L);
        annualBalance.setUser(employee);
        annualBalance.setLeaveType(annualLeave);
        annualBalance.setYear(2026);
        annualBalance.setTotalDays(new BigDecimal("7.0"));
        annualBalance.setUsedDays(BigDecimal.ZERO);
    }

    // ─── apply ───────────────────────────────────────────────

    @Nested
    @DisplayName("apply()")
    class ApplyTests {

        @Test
        @DisplayName("正常請假申請 — 狀態為 PENDING，含代理人")
        void apply_success() {
            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 16, 18, 0));
            request.setReason("家中有事");
            request.setAgentId(2L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(userRepository.findById(2L)).thenReturn(Optional.of(agent));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(200L);
                return lr;
            });

            LeaveResponse response = leaveService.apply(1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getLeaveTypeName()).isEqualTo("特休");
            assertThat(response.getAgentId()).isEqualTo(2L);
            assertThat(response.getAgentName()).isEqualTo("張代理人");
            assertThat(response.getReason()).isEqualTo("家中有事");

            verify(mailService).sendLeaveApplicationNotification(
                    eq("manager@test.com"), eq("李員工"), eq("特休"),
                    anyString(), anyString(), eq("張代理人"));
        }

        @Test
        @DisplayName("請假申請不指定代理人 — agentId 為 null")
        void apply_withoutAgent() {
            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            request.setReason("私人原因");
            // agentId 為 null

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(201L);
                return lr;
            });

            LeaveResponse response = leaveService.apply(1L, request);

            assertThat(response.getAgentId()).isNull();
            assertThat(response.getAgentName()).isNull();
        }

        @Test
        @DisplayName("使用者不存在 — 拋出 ResourceNotFoundException")
        void apply_userNotFound() {
            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            request.setReason("測試");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leaveService.apply(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("使用者不存在");
        }

        @Test
        @DisplayName("假別不存在 — 拋出 ResourceNotFoundException")
        void apply_leaveTypeNotFound() {
            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(999L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            request.setReason("測試");

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leaveService.apply(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("假別不存在");
        }

        @Test
        @DisplayName("代理人不存在 — 拋出 ResourceNotFoundException")
        void apply_agentNotFound() {
            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            request.setReason("測試");
            request.setAgentId(999L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leaveService.apply(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("代理人不存在");
        }

        @Test
        @DisplayName("員工沒有主管 — 不寄 Email 通知")
        void apply_noManager() {
            employee.setManager(null);

            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            request.setReason("測試");

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(202L);
                return lr;
            });

            leaveService.apply(1L, request);

            verify(mailService, never()).sendLeaveApplicationNotification(
                    anyString(), anyString(), anyString(), anyString(), anyString(), any());
        }
    }

    // ─── approve ─────────────────────────────────────────────

    @Nested
    @DisplayName("approve()")
    class ApproveTests {

        private LeaveRequest pendingLeave;

        @BeforeEach
        void setUpLeave() {
            pendingLeave = new LeaveRequest();
            pendingLeave.setId(200L);
            pendingLeave.setUser(employee);
            pendingLeave.setLeaveType(annualLeave);
            pendingLeave.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            pendingLeave.setEndTime(LocalDateTime.of(2026, 6, 16, 18, 0));
            pendingLeave.setReason("家中有事");
            pendingLeave.setStatus(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("核准請假 — 狀態變 APPROVED，餘額扣減 2 天")
        void approve_success() {
            // 16 小時 = 精確 2.0 天（09:00 → 隔日 01:00）
            pendingLeave.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            pendingLeave.setEndTime(LocalDateTime.of(2026, 6, 16, 1, 0));

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.of(annualBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveResponse response = leaveService.approve(200L, 10L);

            assertThat(response.getStatus()).isEqualTo("APPROVED");
            assertThat(response.getApprovedById()).isEqualTo(10L);
            assertThat(response.getApprovedByName()).isEqualTo("王主管");
            assertThat(response.getApprovedAt()).isNotNull();

            // 16 小時 / 8 = 2.0 天
            assertThat(annualBalance.getUsedDays()).isEqualByComparingTo(new BigDecimal("2.0"));

            verify(mailService).sendLeaveApprovalResult(eq("emp@test.com"), eq(true), eq("特休"),
                    eq("2026-06-15T09:00"), eq("2026-06-16T01:00"));
        }

        @Test
        @DisplayName("核准 1 天請假 — 餘額扣減 1 天")
        void approve_oneDay() {
            // 精確 8 小時 = 1.0 天（09:00 → 17:00）
            pendingLeave.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            pendingLeave.setEndTime(LocalDateTime.of(2026, 6, 15, 17, 0));

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.of(annualBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.approve(200L, 10L);

            // 8 小時 / 8 = 1.0 天
            assertThat(annualBalance.getUsedDays()).isEqualByComparingTo(new BigDecimal("1.0"));
        }

        @Test
        @DisplayName("核准半天請假 — 餘額扣減 0.5 天")
        void approve_halfDay() {
            pendingLeave.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            pendingLeave.setEndTime(LocalDateTime.of(2026, 6, 15, 13, 0));

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.of(annualBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.approve(200L, 10L);

            // 4 小時 = 0.5 天
            assertThat(annualBalance.getUsedDays()).isEqualByComparingTo(new BigDecimal("0.5"));
        }

        @Test
        @DisplayName("假別餘額不足 — 拋出「假別餘額不足」")
        void approve_insufficientBalance() {
            // 16 小時 = 2.0 天，但餘額只有 0.5 天
            pendingLeave.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            pendingLeave.setEndTime(LocalDateTime.of(2026, 6, 16, 1, 0));
            annualBalance.setTotalDays(new BigDecimal("1.0"));
            annualBalance.setUsedDays(new BigDecimal("0.5"));

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.of(annualBalance));

            assertThatThrownBy(() -> leaveService.approve(200L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("假別餘額不足");

            // 狀態不應改變
            assertThat(pendingLeave.getStatus()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("找不到年度假別餘額 — 拋出「找不到該年度假別餘額」")
        void approve_noBalanceRecord() {
            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> leaveService.approve(200L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("找不到該年度假別餘額");
        }

        @Test
        @DisplayName("重複核准 — 拋出「此請假單已簽核」")
        void approve_alreadyApproved() {
            pendingLeave.setStatus(RequestStatus.APPROVED);

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.approve(200L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("此請假單已簽核");

            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("核准已被駁回的假單 — 拋出「此請假單已簽核」")
        void approve_alreadyRejected() {
            pendingLeave.setStatus(RequestStatus.REJECTED);

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.approve(200L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("此請假單已簽核");
        }

        @Test
        @DisplayName("請假單不存在 — 拋出 ResourceNotFoundException")
        void approve_leaveNotFound() {
            when(leaveRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leaveService.approve(999L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("請假單不存在");
        }

        @Test
        @DisplayName("餘額恰好等於申請天數 — 核准成功")
        void approve_exactBalance() {
            // 使用 8 小時 = 1.0 天，餘額也設為 1.0
            pendingLeave.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            pendingLeave.setEndTime(LocalDateTime.of(2026, 6, 15, 17, 0));
            annualBalance.setTotalDays(new BigDecimal("1.0"));
            annualBalance.setUsedDays(BigDecimal.ZERO);

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.of(annualBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveResponse response = leaveService.approve(200L, 10L);

            assertThat(response.getStatus()).isEqualTo("APPROVED");
            assertThat(annualBalance.getUsedDays()).isEqualByComparingTo(new BigDecimal("1.0"));
            // remaining = 1.0 - 1.0 = 0.0
            assertThat(annualBalance.getTotalDays().subtract(annualBalance.getUsedDays()))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ─── reject ──────────────────────────────────────────────

    @Nested
    @DisplayName("reject()")
    class RejectTests {

        private LeaveRequest pendingLeave;

        @BeforeEach
        void setUpLeave() {
            pendingLeave = new LeaveRequest();
            pendingLeave.setId(200L);
            pendingLeave.setUser(employee);
            pendingLeave.setLeaveType(annualLeave);
            pendingLeave.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            pendingLeave.setEndTime(LocalDateTime.of(2026, 6, 16, 18, 0));
            pendingLeave.setReason("家中有事");
            pendingLeave.setStatus(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("駁回請假 — 狀態變 REJECTED，不扣減餘額")
        void reject_success() {
            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveResponse response = leaveService.reject(200L, 10L);

            assertThat(response.getStatus()).isEqualTo("REJECTED");
            assertThat(response.getApprovedById()).isEqualTo(10L);
            assertThat(response.getApprovedByName()).isEqualTo("王主管");
            assertThat(response.getApprovedAt()).isNotNull();

            verify(mailService).sendLeaveApprovalResult(eq("emp@test.com"), eq(false), eq("特休"),
                    eq("2026-06-15T09:00"), eq("2026-06-16T18:00"));
        }

        @Test
        @DisplayName("駁回不扣減假別餘額")
        void reject_doesNotDeductBalance() {
            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.reject(200L, 10L);

            // 餘額相關的 repo 不應被呼叫
            verify(leaveBalanceRepository, never()).findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt());
        }

        @Test
        @DisplayName("重複駁回 — 拋出「此請假單已簽核」")
        void reject_alreadyProcessed() {
            pendingLeave.setStatus(RequestStatus.REJECTED);

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.reject(200L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("此請假單已簽核");

            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("請假單不存在 — 拋出 ResourceNotFoundException")
        void reject_leaveNotFound() {
            when(leaveRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leaveService.reject(999L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("請假單不存在");
        }
    }

    // ─── getMyLeaves ─────────────────────────────────────────

    @Nested
    @DisplayName("getMyLeaves()")
    class GetMyLeavesTests {

        @Test
        @DisplayName("查詢自己的請假記錄 — 回傳列表")
        void getMyLeaves_success() {
            LeaveRequest lr1 = new LeaveRequest();
            lr1.setId(200L);
            lr1.setUser(employee);
            lr1.setLeaveType(annualLeave);
            lr1.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            lr1.setEndTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            lr1.setReason("休假");
            lr1.setStatus(RequestStatus.PENDING);

            when(leaveRequestRepository.findByUserId(1L)).thenReturn(List.of(lr1));

            List<LeaveResponse> responses = leaveService.getMyLeaves(1L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getLeaveTypeName()).isEqualTo("特休");
            assertThat(responses.get(0).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("無請假記錄 — 回傳空列表")
        void getMyLeaves_empty() {
            when(leaveRequestRepository.findByUserId(1L)).thenReturn(List.of());

            List<LeaveResponse> responses = leaveService.getMyLeaves(1L);

            assertThat(responses).isEmpty();
        }
    }

    // ─── getPendingLeaves ────────────────────────────────────

    @Nested
    @DisplayName("getPendingLeaves()")
    class GetPendingLeavesTests {

        @Test
        @DisplayName("主管查詢待簽核假單 — 回傳列表")
        void getPendingLeaves_success() {
            LeaveRequest lr = new LeaveRequest();
            lr.setId(200L);
            lr.setUser(employee);
            lr.setLeaveType(sickLeave);
            lr.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            lr.setEndTime(LocalDateTime.of(2026, 6, 15, 18, 0));
            lr.setReason("身體不適");
            lr.setStatus(RequestStatus.PENDING);

            when(leaveRequestRepository.findByStatusAndUser_ManagerId(RequestStatus.PENDING, 10L))
                    .thenReturn(List.of(lr));

            List<LeaveResponse> responses = leaveService.getPendingLeaves(10L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getLeaveTypeName()).isEqualTo("病假");
        }

        @Test
        @DisplayName("無待簽核假單 — 回傳空列表")
        void getPendingLeaves_empty() {
            when(leaveRequestRepository.findByStatusAndUser_ManagerId(RequestStatus.PENDING, 10L))
                    .thenReturn(List.of());

            List<LeaveResponse> responses = leaveService.getPendingLeaves(10L);

            assertThat(responses).isEmpty();
        }
    }

    // ─── getBalance ──────────────────────────────────────────

    @Nested
    @DisplayName("getBalance()")
    class GetBalanceTests {

        @Test
        @DisplayName("查詢假別餘額 — 回傳 total/used/remaining")
        void getBalance_success() {
            LeaveBalance sickBalance = new LeaveBalance();
            sickBalance.setId(2L);
            sickBalance.setUser(employee);
            sickBalance.setLeaveType(sickLeave);
            sickBalance.setYear(2026);
            sickBalance.setTotalDays(new BigDecimal("30.0"));
            sickBalance.setUsedDays(new BigDecimal("2.5"));

            when(leaveBalanceRepository.findByUserIdAndYear(1L, 2026))
                    .thenReturn(List.of(annualBalance, sickBalance));

            List<LeaveBalanceResponse> responses = leaveService.getBalance(1L, 2026);

            assertThat(responses).hasSize(2);

            LeaveBalanceResponse annualResp = responses.stream()
                    .filter(r -> r.getLeaveTypeCode().equals("ANNUAL"))
                    .findFirst().orElseThrow();
            assertThat(annualResp.getTotalDays()).isEqualByComparingTo("7.0");
            assertThat(annualResp.getUsedDays()).isEqualByComparingTo("0.0");
            assertThat(annualResp.getRemainingDays()).isEqualByComparingTo("7.0");

            LeaveBalanceResponse sickResp = responses.stream()
                    .filter(r -> r.getLeaveTypeCode().equals("SICK"))
                    .findFirst().orElseThrow();
            assertThat(sickResp.getTotalDays()).isEqualByComparingTo("30.0");
            assertThat(sickResp.getUsedDays()).isEqualByComparingTo("2.5");
            assertThat(sickResp.getRemainingDays()).isEqualByComparingTo("27.5");
        }

        @Test
        @DisplayName("year=0 時自動使用當前年度")
        void getBalance_defaultYear() {
            when(leaveBalanceRepository.findByUserIdAndYear(eq(1L), eq(java.time.Year.now().getValue())))
                    .thenReturn(List.of(annualBalance));

            List<LeaveBalanceResponse> responses = leaveService.getBalance(1L, 0);

            assertThat(responses).hasSize(1);
            verify(leaveBalanceRepository).findByUserIdAndYear(eq(1L), eq(java.time.Year.now().getValue()));
        }

        @Test
        @DisplayName("無餘額記錄 — 回傳空列表")
        void getBalance_empty() {
            when(leaveBalanceRepository.findByUserIdAndYear(1L, 2025))
                    .thenReturn(List.of());

            List<LeaveBalanceResponse> responses = leaveService.getBalance(1L, 2025);

            assertThat(responses).isEmpty();
        }
    }

    // ─── 預設代理人回退 ──────────────────────────────────────────

    @Nested
    @DisplayName("預設代理人回退（User.agent）")
    class DefaultAgentTests {

        @Test
        @DisplayName("請假未指定代理人但使用者有預設代理人 → 自動使用預設代理人")
        void apply_usesDefaultAgentWhenNotSpecified() {
            employee.setAgent(agent);

            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 17, 0));
            request.setReason("休假");
            // agentId 為 null → 應使用 user 的預設代理人

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(300L);
                return lr;
            });

            LeaveResponse response = leaveService.apply(1L, request);

            assertThat(response.getAgentId()).isEqualTo(2L);
            assertThat(response.getAgentName()).isEqualTo("張代理人");

            // Email 通知也應包含預設代理人
            verify(mailService).sendLeaveApplicationNotification(
                    eq("manager@test.com"), eq("李員工"), eq("特休"),
                    anyString(), anyString(), eq("張代理人"));
        }

        @Test
        @DisplayName("明確指定代理人 → 覆蓋預設代理人")
        void apply_explicitAgentOverridesDefault() {
            employee.setAgent(agent); // 預設代理人為「張代理人」

            // 明確指定 admin（id=10）為代理人
            User anotherUser = new User();
            anotherUser.setId(10L);
            anotherUser.setName("王主管");
            anotherUser.setEmail("manager@test.com");

            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 17, 0));
            request.setReason("休假");
            request.setAgentId(10L); // 明確指定

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(userRepository.findById(10L)).thenReturn(Optional.of(anotherUser));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(301L);
                return lr;
            });

            LeaveResponse response = leaveService.apply(1L, request);

            // 應使用明確指定的代理人，而非預設
            assertThat(response.getAgentId()).isEqualTo(10L);
            assertThat(response.getAgentName()).isEqualTo("王主管");
        }

        @Test
        @DisplayName("無預設代理人且未指定 → agentId 為 null")
        void apply_noDefaultNoExplicit() {
            employee.setAgent(null); // 無預設代理人

            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 17, 0));
            request.setReason("休假");

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(302L);
                return lr;
            });

            LeaveResponse response = leaveService.apply(1L, request);

            assertThat(response.getAgentId()).isNull();
            assertThat(response.getAgentName()).isNull();
        }

        @Test
        @DisplayName("請假含預設代理人 → Email 通知包含代理人姓名")
        void apply_defaultAgent_emailNotification() {
            employee.setAgent(agent);

            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 17, 0));
            request.setReason("家中有事");

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(303L);
                return lr;
            });

            leaveService.apply(1L, request);

            verify(mailService).sendLeaveApplicationNotification(
                    eq("manager@test.com"), eq("李員工"), eq("特休"),
                    anyString(), anyString(), eq("張代理人"));
        }

        @Test
        @DisplayName("無代理人 → Email 通知代理人欄位為 null")
        void apply_noAgent_emailNotificationNull() {
            employee.setAgent(null);

            LeaveApplyRequest request = new LeaveApplyRequest();
            request.setLeaveTypeId(100L);
            request.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
            request.setEndTime(LocalDateTime.of(2026, 6, 15, 17, 0));
            request.setReason("休假");

            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(100L)).thenReturn(Optional.of(annualLeave));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(304L);
                return lr;
            });

            leaveService.apply(1L, request);

            verify(mailService).sendLeaveApplicationNotification(
                    eq("manager@test.com"), eq("李員工"), eq("特休"),
                    anyString(), anyString(), eq(null));
        }
    }

    // ─── calculateLeaveDays（邊界案例）────────────────────────

    @Nested
    @DisplayName("請假天數計算邊界案例")
    class CalculateLeaveDaysTests {

        private LeaveRequest createLeaveRequest(LocalDateTime start, LocalDateTime end) {
            LeaveRequest lr = new LeaveRequest();
            lr.setId(200L);
            lr.setUser(employee);
            lr.setLeaveType(annualLeave);
            lr.setStartTime(start);
            lr.setEndTime(end);
            lr.setReason("測試");
            lr.setStatus(RequestStatus.PENDING);
            return lr;
        }

        @Test
        @DisplayName("1 小時請假 = 0.1 天（四捨五入）")
        void approve_oneHour() {
            LeaveRequest lr = createLeaveRequest(
                    LocalDateTime.of(2026, 6, 15, 9, 0),
                    LocalDateTime.of(2026, 6, 15, 10, 0));
            // 1 hour / 8 = 0.125 → HALF_UP scale 1 = 0.1

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(lr));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.of(annualBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.approve(200L, 10L);

            // 1 hour / 8 hours = 0.125 → HALF_UP scale 1 = 0.1
            assertThat(annualBalance.getUsedDays()).isEqualByComparingTo(new BigDecimal("0.1"));
        }

        @Test
        @DisplayName("12 小時請假 = 1.5 天")
        void approve_twelveHours() {
            LeaveRequest lr = createLeaveRequest(
                    LocalDateTime.of(2026, 6, 15, 9, 0),
                    LocalDateTime.of(2026, 6, 15, 21, 0));

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(lr));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.of(annualBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.approve(200L, 10L);

            // 12 hours / 8 hours = 1.5
            assertThat(annualBalance.getUsedDays()).isEqualByComparingTo(new BigDecimal("1.5"));
        }

        @Test
        @DisplayName("跨 2 天共 16 小時 = 2.0 天")
        void approve_twoDays() {
            // 精確 16 小時 = 2.0 天（09:00 → 隔日 01:00）
            LeaveRequest lr = createLeaveRequest(
                    LocalDateTime.of(2026, 6, 15, 9, 0),
                    LocalDateTime.of(2026, 6, 16, 1, 0));

            when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(lr));
            when(userRepository.getReferenceById(10L)).thenReturn(manager);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 100L, 2026))
                    .thenReturn(Optional.of(annualBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.approve(200L, 10L);

            // 16 hours / 8 = 2.0
            assertThat(annualBalance.getUsedDays()).isEqualByComparingTo(new BigDecimal("2.0"));
        }
    }
}
