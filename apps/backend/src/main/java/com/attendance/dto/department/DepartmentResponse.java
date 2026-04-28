package com.attendance.dto.department;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
