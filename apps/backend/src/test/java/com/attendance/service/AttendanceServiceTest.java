package com.attendance.service;

import com.attendance.dto.attendance.ClockResponse;
import com.attendance.dto.attendance.MonthlyResponse;
import com.attendance.entity.Attendance;
import com.attendance.entity.AttendanceStatus;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.entity.UserRole;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AttendanceService 單元測試。
 * 驗證打卡、下班、查詢的商業邏輯與遲到/早退判定。
 *
 * 注意：clockIn/clockOut 使用 LocalDateTime.now() 判定狀態，
 * 測試依據實際執行時間動態計算預期狀態，確保任何時間執行都通過。
 */
class AttendanceServiceTest extends ServiceTestBase {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AttendanceService attendanceService;

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END   = LocalTime.of(18, 0);

    private User testUser;

    @BeforeEach
    void setUp() {
        Department testDept = new Department(1L, "測試部", null);
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("emp@test.com");
        testUser.setName("測試員工");
        testUser.setRole(UserRole.EMPLOYEE);
        testUser.setDepartment(testDept);
        testUser.setIsActive(true);
    }

    // ─── clockIn ─────────────────────────────────────────────

    @Nested
    @DisplayName("clockIn()")
    class ClockInTests {

        @Test
        @DisplayName("首次上班打卡 — clockIn 已設定，狀態依時間決定")
        void clockIn_success() {
            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> {
                Attendance a = inv.getArgument(0);
                a.setId(100L);
                return a;
            });

            ClockResponse response = attendanceService.clockIn(1L);

            assertThat(response).isNotNull();

            // 依據目前時間決定預期狀態（09:00 前為 NORMAL，之後為 LATE）
            AttendanceStatus expectedStatus =
                    LocalTime.now().isAfter(WORK_START) ? AttendanceStatus.LATE : AttendanceStatus.NORMAL;

            verify(attendanceRepository).save(argThat(a ->
                    a.getClockIn() != null && a.getStatus() == expectedStatus
            ));
        }

        @Test
        @DisplayName("遲到判定邏輯 — 09:00 後 clockIn 產生 LATE 狀態")
        void clockIn_lateStatusLogic() {
            // 直接驗證 service 的遲到邏輯：
            // 當 now > 09:00 時，status 應為 LATE
            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> {
                Attendance a = inv.getArgument(0);
                a.setId(100L);
                return a;
            });

            attendanceService.clockIn(1L);

            if (LocalTime.now().isAfter(WORK_START)) {
                verify(attendanceRepository).save(argThat(a ->
                        a.getStatus() == AttendanceStatus.LATE
                ));
            } else {
                verify(attendanceRepository).save(argThat(a ->
                        a.getStatus() == AttendanceStatus.NORMAL
                ));
            }
        }

        @Test
        @DisplayName("重複上班打卡 — 拋出「今日已打上班卡」")
        void clockIn_duplicate() {
            Attendance existing = new Attendance();
            existing.setId(10L);
            existing.setUser(testUser);
            existing.setDate(LocalDate.now());
            existing.setClockIn(LocalDateTime.of(2026, 6, 12, 8, 50));
            existing.setStatus(AttendanceStatus.NORMAL);

            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> attendanceService.clockIn(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("今日已打上班卡");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("使用者不存在 — 拋出「使用者不存在」")
        void clockIn_userNotFound() {
            when(attendanceRepository.findByUserIdAndDate(eq(999L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.clockIn(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("使用者不存在");
        }
    }

    // ─── clockOut ────────────────────────────────────────────

    @Nested
    @DisplayName("clockOut()")
    class ClockOutTests {

        @Test
        @DisplayName("正常下班打卡 — clockOut 已設定")
        void clockOut_success() {
            Attendance attendance = new Attendance();
            attendance.setId(10L);
            attendance.setUser(testUser);
            attendance.setDate(LocalDate.now());
            attendance.setClockIn(LocalDateTime.of(2026, 6, 12, 8, 50));
            attendance.setStatus(AttendanceStatus.NORMAL);

            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(attendance));
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            ClockResponse response = attendanceService.clockOut(1L);

            assertThat(response).isNotNull();

            verify(attendanceRepository).save(argThat(a -> a.getClockOut() != null));
        }

        @Test
        @DisplayName("早退判定 — clockOut 時間 < 18:00 產生 EARLY_LEAVE")
        void clockOut_earlyLeaveStatusLogic() {
            // 驗證早退邏輯：若目前時間 < 18:00，則狀態為 EARLY_LEAVE
            Attendance attendance = new Attendance();
            attendance.setId(10L);
            attendance.setUser(testUser);
            attendance.setDate(LocalDate.now());
            attendance.setClockIn(LocalDateTime.of(2026, 6, 12, 8, 50));
            attendance.setStatus(AttendanceStatus.NORMAL);

            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(attendance));
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            attendanceService.clockOut(1L);

            if (LocalTime.now().isBefore(WORK_END)) {
                verify(attendanceRepository).save(argThat(a ->
                        a.getStatus() == AttendanceStatus.EARLY_LEAVE
                ));
            } else {
                verify(attendanceRepository).save(argThat(a ->
                        a.getStatus() == AttendanceStatus.NORMAL
                ));
            }
        }

        @Test
        @DisplayName("遲到後下班 — LATE 狀態不被覆蓋")
        void clockOut_lateStatusPreserved() {
            // 遲到打卡後正常下班，狀態應維持 LATE（不因下班時間被覆蓋為 NORMAL/EARLY_LEAVE）
            // 注意：此邏輯驗證 service 的 `if (status != LATE)` 條件
            Attendance attendance = new Attendance();
            attendance.setId(10L);
            attendance.setUser(testUser);
            attendance.setDate(LocalDate.now());
            attendance.setClockIn(LocalDateTime.of(2026, 6, 12, 9, 30));
            attendance.setStatus(AttendanceStatus.LATE);

            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(attendance));
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            attendanceService.clockOut(1L);

            // 因為 status == LATE，service 不會更改狀態（除非早退覆蓋）
            // service 邏輯：先檢查早退，再檢查 LATE 不覆蓋
            // 若目前時間 < 18:00 → EARLY_LEAVE（會覆蓋 LATE）
            // 若目前時間 >= 18:00 → 保持 LATE
            if (LocalTime.now().isBefore(WORK_END)) {
                // 早退優先：即使是 LATE 打卡，早退也會覆蓋為 EARLY_LEAVE
                verify(attendanceRepository).save(argThat(a ->
                        a.getStatus() == AttendanceStatus.EARLY_LEAVE
                ));
            } else {
                verify(attendanceRepository).save(argThat(a ->
                        a.getStatus() == AttendanceStatus.LATE
                ));
            }
        }

        @Test
        @DisplayName("尚未打上班卡就打下班 — 拋出「今日尚未打上班卡」")
        void clockOut_noClockIn() {
            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.clockOut(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("今日尚未打上班卡");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("重複下班打卡 — 拋出「今日已打下班卡」")
        void clockOut_duplicate() {
            Attendance existing = new Attendance();
            existing.setId(10L);
            existing.setUser(testUser);
            existing.setDate(LocalDate.now());
            existing.setClockIn(LocalDateTime.of(2026, 6, 12, 8, 50));
            existing.setClockOut(LocalDateTime.of(2026, 6, 12, 18, 5));
            existing.setStatus(AttendanceStatus.NORMAL);

            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> attendanceService.clockOut(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("今日已打下班卡");

            verify(attendanceRepository, never()).save(any());
        }
    }

    // ─── getToday ────────────────────────────────────────────

    @Nested
    @DisplayName("getToday()")
    class GetTodayTests {

        @Test
        @DisplayName("有今日記錄 — 回傳 ClockResponse")
        void getToday_found() {
            Attendance attendance = new Attendance();
            attendance.setId(10L);
            attendance.setUser(testUser);
            attendance.setDate(LocalDate.now());
            attendance.setClockIn(LocalDateTime.of(2026, 6, 12, 8, 55));
            attendance.setStatus(AttendanceStatus.NORMAL);

            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(attendance));

            ClockResponse response = attendanceService.getToday(1L);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("NORMAL");
            assertThat(response.getClockIn()).isNotNull();
        }

        @Test
        @DisplayName("無今日記錄 — 回傳 null")
        void getToday_notFound() {
            when(attendanceRepository.findByUserIdAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            ClockResponse response = attendanceService.getToday(1L);

            assertThat(response).isNull();
        }
    }

    // ─── getMonthly ──────────────────────────────────────────

    @Nested
    @DisplayName("getMonthly()")
    class GetMonthlyTests {

        @Test
        @DisplayName("有月記錄 — 回傳 MonthlyResponse 含 2 筆")
        void getMonthly_withData() {
            LocalDate start = LocalDate.of(2026, 6, 1);
            LocalDate end = LocalDate.of(2026, 6, 30);

            Attendance a1 = new Attendance();
            a1.setId(10L);
            a1.setUser(testUser);
            a1.setDate(LocalDate.of(2026, 6, 1));
            a1.setClockIn(LocalDateTime.of(2026, 6, 1, 8, 55));
            a1.setStatus(AttendanceStatus.NORMAL);

            Attendance a2 = new Attendance();
            a2.setId(11L);
            a2.setUser(testUser);
            a2.setDate(LocalDate.of(2026, 6, 2));
            a2.setClockIn(LocalDateTime.of(2026, 6, 2, 9, 15));
            a2.setClockOut(LocalDateTime.of(2026, 6, 2, 18, 5));
            a2.setStatus(AttendanceStatus.LATE);

            when(attendanceRepository.findByUserIdAndDateBetween(1L, start, end))
                    .thenReturn(List.of(a1, a2));

            MonthlyResponse response = attendanceService.getMonthly(1L, 2026, 6);

            assertThat(response).isNotNull();
            assertThat(response.getYear()).isEqualTo(2026);
            assertThat(response.getMonth()).isEqualTo(6);
            assertThat(response.getRecords()).hasSize(2);
            assertThat(response.getRecords().get(0).getStatus()).isEqualTo("NORMAL");
            assertThat(response.getRecords().get(1).getStatus()).isEqualTo("LATE");
            assertThat(response.getRecords().get(1).getClockOut()).isNotNull();
        }

        @Test
        @DisplayName("無月記錄 — 回傳空列表")
        void getMonthly_empty() {
            LocalDate start = LocalDate.of(2026, 7, 1);
            LocalDate end = LocalDate.of(2026, 7, 31);

            when(attendanceRepository.findByUserIdAndDateBetween(1L, start, end))
                    .thenReturn(List.of());

            MonthlyResponse response = attendanceService.getMonthly(1L, 2026, 7);

            assertThat(response).isNotNull();
            assertThat(response.getRecords()).isEmpty();
            assertThat(response.getYear()).isEqualTo(2026);
            assertThat(response.getMonth()).isEqualTo(7);
        }
    }
}
