package com.attendance.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveResponse {

    private Long id;
    private String leaveTypeName;
    private String leaveTypeCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Long agentId;
    private String agentName;
    private String status;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
