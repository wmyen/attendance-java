package com.attendance.dto.leave;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LeaveApplyRequest {

    @NotNull(message = "leaveTypeId 為必填")
    private Long leaveTypeId;

    @NotNull(message = "startTime 為必填")
    private LocalDateTime startTime;

    @NotNull(message = "endTime 為必填")
    private LocalDateTime endTime;

    @NotBlank(message = "reason 為必填")
    private String reason;

    private Long agentId;
}
