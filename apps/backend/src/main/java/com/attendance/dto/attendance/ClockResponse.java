package com.attendance.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClockResponse {

    private Long id;
    private LocalDate date;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private String status;
}
