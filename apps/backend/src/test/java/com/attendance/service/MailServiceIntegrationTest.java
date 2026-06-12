package com.attendance.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * MailService 整合測試。
 * 啟動完整 Spring Context，實際連線 SMTP 寄出 email。
 *
 * ⚠️ 此測試會寄出真實信件，已標記 @Tag("smtp") 排除在預設測試之外。
 * 執行方式：mvn test -Dgroups=smtp
 */
@SpringBootTest
@Tag("smtp")
class MailServiceIntegrationTest {

    @Autowired
    private MailService mailService;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Test
    @DisplayName("實際寄送測試信 — 驗證 SMTP 連線與寄送成功")
    void sendTestEmail_realSmtp() {
        assertThat(mailUsername).isNotBlank();

        // 寄信到自己信箱（發信者 = 收信者）
        assertThatCode(() ->
                mailService.sendNewUserCredentials(
                        mailUsername,
                        "整合測試使用者",
                        "TestPassword123"
                )
        ).doesNotThrowAnyException();

        // @Async 需要等待一下讓非同步執行完成
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("✅ 測試信已寄至: " + mailUsername);
        System.out.println("   請檢查收件匣（或垃圾信件匣）確認收到信件。");
    }
}
