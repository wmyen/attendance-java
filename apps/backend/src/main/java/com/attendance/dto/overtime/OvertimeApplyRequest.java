package com.attendance.dto.overtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OvertimeApplyRequest {

    @NotNull(message = "startTime 為必填")
    private LocalDateTime startTime;

    @NotNull(message = "endTime 為必填")
    private LocalDateTime endTime;

    @NotBlank(message = "reason 為必填")
    private String reason;
}
