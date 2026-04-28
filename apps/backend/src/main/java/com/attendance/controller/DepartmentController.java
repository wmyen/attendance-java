package com.attendance.controller;

import com.attendance.dto.department.DepartmentRequest;
import com.attendance.dto.department.DepartmentResponse;
import com.attendance.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> list() {
        return ResponseEntity.ok(departmentService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> update(@PathVariable @NonNull Long id,
                                                     @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }
}
