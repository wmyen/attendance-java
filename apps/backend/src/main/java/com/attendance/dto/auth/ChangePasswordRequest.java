package com.attendance.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "oldPassword 為必填")
    private String oldPassword;

    @NotBlank(message = "newPassword 為必填")
    @Size(min = 6, max = 20, message = "newPassword 長度須介於 6 至 20 字元")
    private String newPassword;
}
