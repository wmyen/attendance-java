package com.attendance.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank(message = "email 為必填")
    @Email(message = "email 格式不正確")
    private String email;

    @NotBlank(message = "name 為必填")
    @Size(max = 100, message = "name 長度不可超過 100 字元")
    private String name;

    @NotBlank(message = "role 為必填")
    private String role;

    private Long deptId;

    private Long managerId;

    private Long agentId;
}
