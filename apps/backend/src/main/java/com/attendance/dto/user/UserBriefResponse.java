package com.attendance.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 使用者簡要資訊（供代理人選擇下拉選單使用）。
 * 所有已認證使用者皆可存取。
 */
@Getter
@AllArgsConstructor
public class UserBriefResponse {
    private Long id;
    private String name;
    private String email;
}
