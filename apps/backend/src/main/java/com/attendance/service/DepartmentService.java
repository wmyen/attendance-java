package com.attendance.service;

import com.attendance.dto.department.DepartmentRequest;
import com.attendance.dto.department.DepartmentResponse;
import com.attendance.entity.Department;
import com.attendance.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        Department dept = new Department();
        dept.setName(request.getName());
        return toResponse(departmentRepository.save(dept));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("部門不存在"));
        dept.setName(request.getName());
        return toResponse(departmentRepository.save(dept));
    }

    private DepartmentResponse toResponse(Department dept) {
        return new DepartmentResponse(dept.getId(), dept.getName(), dept.getCreatedAt());
    }
}
