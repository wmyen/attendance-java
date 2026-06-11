package com.attendance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 整合測試基礎類別。
 * 使用 H2 in-memory database 取代 MySQL，避免測試依賴外部服務。
 * 每個測試方法執行後自動回滾交易，確保測試隔離。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTest {
}
