package com.attendance.service;

import com.attendance.dto.attendance.ClockResponse;
import com.attendance.dto.attendance.MonthlyResponse;
import com.attendance.entity.Attendance;
import com.attendance.entity.AttendanceStatus;
import com.attendance.entity.User;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClockResponse clockIn(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));
                    Attendance a = new Attendance();
                    a.setUser(user);
                    a.setDate(today);
                    return a;
                });

        if (attendance.getClockIn() != null) {
            throw new IllegalArgumentException("今日已打上班卡");
        }

        attendance.setClockIn(now);
        attendance.setStatus(now.toLocalTime().isAfter(WORK_START_TIME)
                ? AttendanceStatus.LATE
                : AttendanceStatus.NORMAL);

        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public ClockResponse clockOut(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository.findByUserIdAndDate(userId, today)
                .orElseThrow(() -> new IllegalArgumentException("今日尚未打上班卡"));

        if (attendance.getClockOut() != null) {
            throw new IllegalArgumentException("今日已打下班卡");
        }

        attendance.setClockOut(now);

        if (now.toLocalTime().isBefore(WORK_END_TIME)) {
            attendance.setStatus(AttendanceStatus.EARLY_LEAVE);
        } else if (attendance.getStatus() != AttendanceStatus.LATE) {
            attendance.setStatus(AttendanceStatus.NORMAL);
        }

        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public ClockResponse getToday(Long userId) {
        return attendanceRepository.findByUserIdAndDate(userId, LocalDate.now())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public MonthlyResponse getMonthly(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<ClockResponse> records = attendanceRepository
                .findByUserIdAndDateBetween(userId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .toList();

        return new MonthlyResponse(records, year, month);
    }

    private ClockResponse toResponse(Attendance a) {
        return new ClockResponse(
                a.getId(), a.getDate(), a.getClockIn(), a.getClockOut(), a.getStatus().name());
    }
}
