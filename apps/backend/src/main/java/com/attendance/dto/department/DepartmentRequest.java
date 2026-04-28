package com.attendance.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRequest {

    @NotBlank(message = "name 為必填")
    @Size(max = 100, message = "name 長度不可超過 100 字元")
    private String name;
}
