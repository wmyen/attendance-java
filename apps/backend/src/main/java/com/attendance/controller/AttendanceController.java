package com.attendance.controller;

import com.attendance.dto.attendance.ClockResponse;
import com.attendance.dto.attendance.MonthlyResponse;
import com.attendance.entity.UserRole;
import com.attendance.security.CustomUserDetails;
import com.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ClockResponse> clockIn(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.clockIn(userDetails.getId()));
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ClockResponse> clockOut(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.clockOut(userDetails.getId()));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ClockResponse> today(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ClockResponse response = attendanceService.getToday(userDetails.getId());
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<MonthlyResponse> monthly(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(required = false) Long userId) {

        YearMonth now = YearMonth.now();
        if (year == 0) year = now.getYear();
        if (month == 0) month = now.getMonthValue();

        String role = userDetails.getRole();
        Long targetUserId = userDetails.getId();

        if (userId != null && (UserRole.ADMIN.name().equals(role) || UserRole.MANAGER.name().equals(role))) {
            targetUserId = userId;
        }

        return ResponseEntity.ok(attendanceService.getMonthly(targetUserId, year, month));
    }
}
