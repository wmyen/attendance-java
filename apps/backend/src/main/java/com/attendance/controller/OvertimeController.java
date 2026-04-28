package com.attendance.controller;

import com.attendance.dto.overtime.OvertimeApplyRequest;
import com.attendance.dto.overtime.OvertimeResponse;
import com.attendance.security.CustomUserDetails;
import com.attendance.service.OvertimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/overtimes")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OvertimeResponse> apply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OvertimeApplyRequest request) {
        return ResponseEntity.ok(overtimeService.apply(userDetails.getId(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<OvertimeResponse>> myOvertimes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(overtimeService.getMyOvertimes(userDetails.getId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<OvertimeResponse>> pendingOvertimes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(overtimeService.getPendingOvertimes(userDetails.getId()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<OvertimeResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(overtimeService.approve(id, userDetails.getId()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<OvertimeResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(overtimeService.reject(id, userDetails.getId()));
    }
}
