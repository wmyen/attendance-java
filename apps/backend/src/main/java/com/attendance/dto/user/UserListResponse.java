package com.attendance.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UserListResponse {

    private List<UserResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
