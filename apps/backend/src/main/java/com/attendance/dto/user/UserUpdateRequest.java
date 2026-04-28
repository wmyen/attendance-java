package com.attendance.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @Size(max = 100, message = "name 長度不可超過 100 字元")
    private String name;

    private String role;

    private Long deptId;

    private Long managerId;

    private Long agentId;
}
