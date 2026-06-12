package com.attendance.service;

import com.attendance.dto.overtime.OvertimeApplyRequest;
import com.attendance.dto.overtime.OvertimeResponse;
import com.attendance.entity.*;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.LeaveBalanceRepository;
import com.attendance.repository.LeaveTypeRepository;
import com.attendance.repository.OvertimeRequestRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeRequestRepository overtimeRequestRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    private static final BigDecimal HOURS_PER_DAY = BigDecimal.valueOf(8);

    @Transactional
    public OvertimeResponse apply(@NonNull Long userId, OvertimeApplyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        OvertimeRequest overtimeRequest = new OvertimeRequest();
        overtimeRequest.setUser(user);
        overtimeRequest.setStartTime(request.getStartTime());
        overtimeRequest.setEndTime(request.getEndTime());
        overtimeRequest.setReason(request.getReason());

        OvertimeResponse response = toResponse(overtimeRequestRepository.save(overtimeRequest));

        if (user.getManager() != null) {
            mailService.sendOvertimeApplicationNotification(
                    user.getManager().getEmail(),
                    user.getName(),
                    overtimeRequest.getStartTime().toString(),
                    overtimeRequest.getEndTime().toString()
            );
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<OvertimeResponse> getMyOvertimes(@NonNull Long userId) {
        return overtimeRequestRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OvertimeResponse> getPendingOvertimes(@NonNull Long managerId) {
        return overtimeRequestRepository.findByStatusAndUser_ManagerId(RequestStatus.PENDING, managerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OvertimeResponse approve(@NonNull Long requestId, @NonNull Long managerId) {
        OvertimeRequest req = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("加班申請不存在"));
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("此加班申請已簽核，無法重複操作");
        }

        User manager = userRepository.getReferenceById(managerId);

        req.setStatus(RequestStatus.APPROVED);
        req.setApprovedBy(manager);
        req.setApprovedAt(LocalDateTime.now());

        // 補休自動產生：計算加班時數 → 轉換為天數 → 累加至 COMPENSATORY 假別餘額
        generateCompensatoryLeave(req);

        OvertimeResponse response = toResponse(overtimeRequestRepository.save(req));
        mailService.sendOvertimeApprovalResult(req.getUser().getEmail(), true);
        return response;
    }

    @Transactional
    public OvertimeResponse reject(@NonNull Long requestId, @NonNull Long managerId) {
        OvertimeRequest req = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("加班申請不存在"));
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("此加班申請已簽核，無法重複操作");
        }

        User manager = userRepository.getReferenceById(managerId);

        req.setStatus(RequestStatus.REJECTED);
        req.setApprovedBy(manager);
        req.setApprovedAt(LocalDateTime.now());

        OvertimeResponse response = toResponse(overtimeRequestRepository.save(req));
        mailService.sendOvertimeApprovalResult(req.getUser().getEmail(), false);
        return response;
    }

    /**
     * 加班核准時自動產生補休餘額。
     * 計算加班時數 → 轉換為天數（8 小時 = 1 天）→ 累加至該年度的 COMPENSATORY 假別餘額。
     */
    private void generateCompensatoryLeave(OvertimeRequest overtimeRequest) {
        leaveTypeRepository.findByCode("COMPENSATORY").ifPresent(compensatoryType -> {
            long totalMinutes = Duration.between(overtimeRequest.getStartTime(), overtimeRequest.getEndTime()).toMinutes();
            BigDecimal hours = BigDecimal.valueOf(totalMinutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            BigDecimal days = hours.divide(HOURS_PER_DAY, 1, RoundingMode.HALF_UP);

            int year = overtimeRequest.getStartTime().getYear();
            Long userId = overtimeRequest.getUser().getId();
            Long typeId = compensatoryType.getId();

            LeaveBalance balance = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeIdAndYear(userId, typeId, year)
                    .orElseGet(() -> {
                        LeaveBalance newBalance = new LeaveBalance();
                        newBalance.setUser(overtimeRequest.getUser());
                        newBalance.setLeaveType(compensatoryType);
                        newBalance.setYear(year);
                        newBalance.setTotalDays(BigDecimal.ZERO);
                        newBalance.setUsedDays(BigDecimal.ZERO);
                        return newBalance;
                    });

            balance.setTotalDays(balance.getTotalDays().add(days));
            leaveBalanceRepository.save(balance);
        });
    }

    private OvertimeResponse toResponse(OvertimeRequest req) {
        OvertimeResponse resp = new OvertimeResponse();
        resp.setId(req.getId());
        resp.setStartTime(req.getStartTime());
        resp.setEndTime(req.getEndTime());
        resp.setReason(req.getReason());
        resp.setStatus(req.getStatus().name());
        resp.setApprovedById(req.getApprovedBy() != null ? req.getApprovedBy().getId() : null);
        resp.setApprovedByName(req.getApprovedBy() != null ? req.getApprovedBy().getName() : null);
        resp.setApprovedAt(req.getApprovedAt());
        resp.setCreatedAt(req.getCreatedAt());
        return resp;
    }
}
