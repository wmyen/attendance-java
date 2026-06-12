package com.attendance.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MailService 單元測試。
 * 驗證每個 email 方法的模板名稱、context 變數、以及異常處理。
 */
class MailServiceTest extends ServiceTestBase {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;

    @InjectMocks private MailService mailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage((Session) null);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        lenient().when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>mock</html>");

        // @Value 在 Mockito 環境不會注入，需手動設定
        ReflectionTestUtils.setField(mailService, "fromAddress", "noreply@test.com");
    }

    // ─── sendNewUserCredentials ───────────────────────────────

    @Nested
    @DisplayName("sendNewUserCredentials()")
    class NewUserTests {

        @Test
        @DisplayName("寄送新使用者帳號通知 — 使用正確模板與變數")
        void sendNewUserCredentials_success() {
            mailService.sendNewUserCredentials("new@test.com", "新員工", "Abc12345");

            ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(eq("email/new-user"), captor.capture());

            Context ctx = captor.getValue();
            assertThat(ctx.getVariable("name")).isEqualTo("新員工");
            assertThat(ctx.getVariable("email")).isEqualTo("new@test.com");
            assertThat(ctx.getVariable("password")).isEqualTo("Abc12345");

            verify(mailSender).send(mimeMessage);
        }
    }

    // ─── sendLeaveApplicationNotification ─────────────────────

    @Nested
    @DisplayName("sendLeaveApplicationNotification()")
    class LeaveApplicationTests {

        @Test
        @DisplayName("寄送請假申請通知 — 包含代理人")
        void sendLeaveNotification_withAgent() {
            mailService.sendLeaveApplicationNotification(
                    "manager@test.com", "李員工", "特休",
                    "2026-06-15T09:00", "2026-06-16T18:00", "王代理人");

            ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(eq("email/leave-application"), captor.capture());

            Context ctx = captor.getValue();
            assertThat(ctx.getVariable("applicantName")).isEqualTo("李員工");
            assertThat(ctx.getVariable("leaveTypeName")).isEqualTo("特休");
            assertThat(ctx.getVariable("startTime")).isEqualTo("2026-06-15T09:00");
            assertThat(ctx.getVariable("endTime")).isEqualTo("2026-06-16T18:00");
            assertThat(ctx.getVariable("agentName")).isEqualTo("王代理人");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("寄送請假申請通知 — 無代理人（agentName 為 null）")
        void sendLeaveNotification_noAgent() {
            mailService.sendLeaveApplicationNotification(
                    "manager@test.com", "李員工", "事假",
                    "2026-06-15T09:00", "2026-06-15T18:00", null);

            ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(eq("email/leave-application"), captor.capture());

            Context ctx = captor.getValue();
            assertThat(ctx.getVariable("agentName")).isNull();
        }
    }

    // ─── sendLeaveApprovalResult ──────────────────────────────

    @Nested
    @DisplayName("sendLeaveApprovalResult()")
    class LeaveResultTests {

        @Test
        @DisplayName("請假核准通知 — approved=true")
        void sendLeaveResult_approved() {
            mailService.sendLeaveApprovalResult(
                    "emp@test.com", true, "特休",
                    "2026-06-15T09:00", "2026-06-16T18:00");

            ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(eq("email/leave-result"), captor.capture());

            Context ctx = captor.getValue();
            assertThat(ctx.getVariable("approved")).isEqualTo(true);
            assertThat(ctx.getVariable("leaveTypeName")).isEqualTo("特休");
            assertThat(ctx.getVariable("startTime")).isEqualTo("2026-06-15T09:00");
            assertThat(ctx.getVariable("endTime")).isEqualTo("2026-06-16T18:00");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("請假駁回通知 — approved=false")
        void sendLeaveResult_rejected() {
            mailService.sendLeaveApprovalResult(
                    "emp@test.com", false, "事假",
                    "2026-06-15T09:00", "2026-06-15T18:00");

            ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(eq("email/leave-result"), captor.capture());

            Context ctx = captor.getValue();
            assertThat(ctx.getVariable("approved")).isEqualTo(false);
            assertThat(ctx.getVariable("leaveTypeName")).isEqualTo("事假");
        }
    }

    // ─── sendOvertimeApplicationNotification ───────────────────

    @Nested
    @DisplayName("sendOvertimeApplicationNotification()")
    class OvertimeApplicationTests {

        @Test
        @DisplayName("寄送加班申請通知 — 使用正確模板與變數")
        void sendOvertimeNotification_success() {
            mailService.sendOvertimeApplicationNotification(
                    "manager@test.com", "李員工",
                    "2026-06-15T18:00", "2026-06-15T22:00");

            ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(eq("email/overtime-application"), captor.capture());

            Context ctx = captor.getValue();
            assertThat(ctx.getVariable("applicantName")).isEqualTo("李員工");
            assertThat(ctx.getVariable("startTime")).isEqualTo("2026-06-15T18:00");
            assertThat(ctx.getVariable("endTime")).isEqualTo("2026-06-15T22:00");

            verify(mailSender).send(mimeMessage);
        }
    }

    // ─── sendOvertimeApprovalResult ────────────────────────────

    @Nested
    @DisplayName("sendOvertimeApprovalResult()")
    class OvertimeResultTests {

        @Test
        @DisplayName("加班核准通知 — approved=true")
        void sendOvertimeResult_approved() {
            mailService.sendOvertimeApprovalResult(
                    "emp@test.com", true,
                    "2026-06-15T18:00", "2026-06-15T22:00");

            ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(eq("email/overtime-result"), captor.capture());

            Context ctx = captor.getValue();
            assertThat(ctx.getVariable("approved")).isEqualTo(true);
            assertThat(ctx.getVariable("startTime")).isEqualTo("2026-06-15T18:00");
            assertThat(ctx.getVariable("endTime")).isEqualTo("2026-06-15T22:00");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("加班駁回通知 — approved=false")
        void sendOvertimeResult_rejected() {
            mailService.sendOvertimeApprovalResult(
                    "emp@test.com", false,
                    "2026-06-15T18:00", "2026-06-15T22:00");

            ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(eq("email/overtime-result"), captor.capture());

            Context ctx = captor.getValue();
            assertThat(ctx.getVariable("approved")).isEqualTo(false);
        }
    }

    // ─── 異常處理 ──────────────────────────────────────────────

    @Nested
    @DisplayName("異常處理")
    class ExceptionTests {

        @Test
        @DisplayName("mailSender.send 拋異常 — 不影響主流程（僅 log）")
        void sendEmail_exception_suppressed() {
            // 重新設定 stub：createMimeMessage 正常回傳，但 send 拋異常
            reset(mailSender);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("SMTP connection failed"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> mailService.sendNewUserCredentials("test@test.com", "測試", "pass"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("templateEngine.process 拋異常 — 不影響主流程")
        void templateEngineException_suppressed() {
            // 重新設定 stub：process 拋異常
            reset(templateEngine);
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenThrow(new RuntimeException("Template not found"));

            assertThatCode(() -> mailService.sendNewUserCredentials("test@test.com", "測試", "pass"))
                    .doesNotThrowAnyException();
        }
    }
}
