package com.attendance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendNewUserCredentials(String toEmail, String name, String password) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("email", toEmail);
        context.setVariable("password", password);
        sendHtmlEmail(toEmail, "考勤系統 — 您的帳號已建立", "email/new-user", context);
    }

    @Async
    public void sendLeaveApplicationNotification(String managerEmail, String applicantName,
            String leaveTypeName, String startTime, String endTime, String agentName) {
        Context context = new Context();
        context.setVariable("applicantName", applicantName);
        context.setVariable("leaveTypeName", leaveTypeName);
        context.setVariable("startTime", startTime);
        context.setVariable("endTime", endTime);
        context.setVariable("agentName", agentName);
        sendHtmlEmail(managerEmail, "考勤系統 — 請假申請通知", "email/leave-application", context);
    }

    @Async
    public void sendLeaveApprovalResult(String applicantEmail, boolean approved, String leaveTypeName) {
        Context context = new Context();
        context.setVariable("approved", approved);
        context.setVariable("leaveTypeName", leaveTypeName);
        String subject = approved ? "考勤系統 — 請假已核准" : "考勤系統 — 請假已駁回";
        sendHtmlEmail(applicantEmail, subject, "email/leave-result", context);
    }

    @Async
    public void sendOvertimeApplicationNotification(String managerEmail, String applicantName,
            String startTime, String endTime) {
        Context context = new Context();
        context.setVariable("applicantName", applicantName);
        context.setVariable("startTime", startTime);
        context.setVariable("endTime", endTime);
        sendHtmlEmail(managerEmail, "考勤系統 — 加班申請通知", "email/overtime-application", context);
    }

    @Async
    public void sendOvertimeApprovalResult(String applicantEmail, boolean approved) {
        Context context = new Context();
        context.setVariable("approved", approved);
        String subject = approved ? "考勤系統 — 加班已核准" : "考勤系統 — 加班已駁回";
        sendHtmlEmail(applicantEmail, subject, "email/overtime-result", context);
    }

    private void sendHtmlEmail(String to, String subject, String template, Context context) {
        try {
            String htmlContent = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
