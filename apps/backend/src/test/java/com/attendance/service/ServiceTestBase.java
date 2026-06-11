package com.attendance.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 單元測試基礎類別（Service 層）。
 * 使用 Mockito 進行依賴隔離，不啟動 Spring Context。
 */
@ExtendWith(MockitoExtension.class)
public abstract class ServiceTestBase {
}
