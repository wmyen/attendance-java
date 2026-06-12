package com.attendance.exception;

/**
 * 認證失敗例外（登入失敗、無效 Token 等）。
 * 對應 HTTP 401 Unauthorized。
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
