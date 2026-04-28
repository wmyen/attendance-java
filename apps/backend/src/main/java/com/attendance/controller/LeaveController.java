package com.attendance.controller;

import com.attendance.dto.leave.LeaveApplyRequest;
import com.attendance.dto.leave.LeaveBalanceResponse;
import com.attendance.dto.leave.LeaveResponse;
import com.attendance.entity.UserRole;
import com.attendance.security.CustomUserDetails;
import com.attendance.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveResponse> apply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LeaveApplyRequest request) {
        return ResponseEntity.ok(leaveService.apply(userDetails.getId(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<LeaveResponse>> myLeaves(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.getMyLeaves(userDetails.getId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<LeaveResponse>> pendingLeaves(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.getPendingLeaves(userDetails.getId()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.approve(id, userDetails.getId()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.reject(id, userDetails.getId()));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<LeaveBalanceResponse>> balance(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(required = false) Long userId) {
        Long targetUserId = resolveTargetUserId(userDetails, userId);
        int targetYear = year == 0 ? Year.now().getValue() : year;
        return ResponseEntity.ok(leaveService.getBalance(targetUserId, targetYear));
    }

    private Long resolveTargetUserId(CustomUserDetails currentUser, Long requestedUserId) {
        boolean isAdmin = UserRole.ADMIN.name().equals(currentUser.getRole());
        if (requestedUserId != null && isAdmin) {
            return requestedUserId;
        }
        return currentUser.getId();
    }
}
