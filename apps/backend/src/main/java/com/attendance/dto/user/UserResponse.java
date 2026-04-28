package com.attendance.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String role;
    private Long deptId;
    private String deptName;
    private Long managerId;
    private String managerName;
    private Long agentId;
    private String agentName;
    private Boolean mustChangePassword;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
