package com.attendance.service;

import com.attendance.dto.department.DepartmentRequest;
import com.attendance.dto.department.DepartmentResponse;
import com.attendance.entity.Department;
import com.attendance.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DepartmentServiceTest extends ServiceTestBase {

    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks private DepartmentService departmentService;

    private Department itDept;
    private Department hrDept;

    @BeforeEach
    void setUp() {
        itDept = new Department(1L, "資訊部", LocalDateTime.of(2026, 1, 1, 0, 0));
        hrDept = new Department(2L, "人事部", LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    // ─── listAll ──────────────────────────────────────────────

    @Nested
    @DisplayName("listAll()")
    class ListAllTests {

        @Test
        @DisplayName("回傳所有部門")
        void listAll() {
            when(departmentRepository.findAll()).thenReturn(List.of(itDept, hrDept));

            List<DepartmentResponse> result = departmentService.listAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("資訊部");
            assertThat(result.get(1).getName()).isEqualTo("人事部");
        }

        @Test
        @DisplayName("無部門 — 回傳空列表")
        void listAllEmpty() {
            when(departmentRepository.findAll()).thenReturn(List.of());

            List<DepartmentResponse> result = departmentService.listAll();

            assertThat(result).isEmpty();
        }
    }

    // ─── create ───────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("成功建立部門")
        void createSuccess() {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("研發部");

            when(departmentRepository.findByName("研發部")).thenReturn(Optional.empty());
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
                Department d = inv.getArgument(0);
                d.setId(5L);
                return d;
            });

            DepartmentResponse resp = departmentService.create(req);

            assertThat(resp.getName()).isEqualTo("研發部");
            verify(departmentRepository).save(argThat(d -> "研發部".equals(d.getName())));
        }

        @Test
        @DisplayName("部門名稱重複 — 拋出異常")
        void createFail_duplicateName() {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("資訊部");

            when(departmentRepository.findByName("資訊部")).thenReturn(Optional.of(itDept));

            assertThatThrownBy(() -> departmentService.create(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("部門名稱已存在");
        }
    }

    // ─── update ───────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("成功更新部門名稱")
        void updateSuccess() {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("科技部");

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));
            when(departmentRepository.save(any(Department.class))).thenReturn(itDept);

            DepartmentResponse resp = departmentService.update(1L, req);

            assertThat(resp.getName()).isEqualTo("科技部");
            verify(departmentRepository).save(argThat(d -> "科技部".equals(d.getName())));
        }

        @Test
        @DisplayName("部門不存在 — 拋出異常")
        void updateNotFound() {
            DepartmentRequest req = new DepartmentRequest();
            req.setName("不存在的部門");

            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.update(999L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("部門不存在");
        }
    }
}
