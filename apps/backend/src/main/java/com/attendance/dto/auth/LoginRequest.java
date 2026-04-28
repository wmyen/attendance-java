package com.attendance.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "email 為必填")
    @Email(message = "email 格式不正確")
    private String email;

    @NotBlank(message = "password 為必填")
    private String password;
}
