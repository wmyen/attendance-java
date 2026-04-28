package com.attendance.service;

import com.attendance.dto.leave.LeaveApplyRequest;
import com.attendance.dto.leave.LeaveBalanceResponse;
import com.attendance.dto.leave.LeaveResponse;
import com.attendance.entity.*;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.LeaveBalanceRepository;
import com.attendance.repository.LeaveRequestRepository;
import com.attendance.repository.LeaveTypeRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    private static final BigDecimal HOURS_PER_DAY = BigDecimal.valueOf(8);

    @Transactional
    public LeaveResponse apply(@NonNull Long userId, LeaveApplyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));
        LeaveType leaveType = leaveTypeRepository.findById(Objects.requireNonNull(request.getLeaveTypeId()))
                .orElseThrow(() -> new ResourceNotFoundException("假別不存在"));

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUser(user);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartTime(request.getStartTime());
        leaveRequest.setEndTime(request.getEndTime());
        leaveRequest.setReason(request.getReason());

        if (request.getAgentId() != null) {
            User agent = userRepository.findById(Objects.requireNonNull(request.getAgentId()))
                    .orElseThrow(() -> new ResourceNotFoundException("代理人不存在"));
            leaveRequest.setAgent(agent);
        }

        LeaveResponse response = toResponse(leaveRequestRepository.save(leaveRequest));

        if (user.getManager() != null) {
            mailService.sendLeaveApplicationNotification(
                    user.getManager().getEmail(),
                    user.getName(),
                    leaveType.getName(),
                    leaveRequest.getStartTime().toString(),
                    leaveRequest.getEndTime().toString(),
                    leaveRequest.getAgent() != null ? leaveRequest.getAgent().getName() : null
            );
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyLeaves(@NonNull Long userId) {
        return leaveRequestRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getPendingLeaves(@NonNull Long managerId) {
        return leaveRequestRepository.findByStatusAndUser_ManagerId(RequestStatus.PENDING, managerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LeaveResponse approve(@NonNull Long requestId, @NonNull Long managerId) {
        LeaveRequest req = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("請假單不存在"));
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("此請假單已簽核，無法重複操作");
        }

        User manager = userRepository.getReferenceById(managerId);

        BigDecimal requestDays = calculateLeaveDays(req.getStartTime(), req.getEndTime());
        int year = req.getStartTime().getYear();

        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(req.getUser().getId(), req.getLeaveType().getId(), year)
                .orElseThrow(() -> new IllegalArgumentException("找不到該年度假別餘額"));

        BigDecimal remaining = balance.getTotalDays().subtract(balance.getUsedDays());
        if (remaining.compareTo(requestDays) < 0) {
            throw new IllegalArgumentException("假別餘額不足");
        }

        balance.setUsedDays(balance.getUsedDays().add(requestDays));

        req.setStatus(RequestStatus.APPROVED);
        req.setApprovedBy(manager);
        req.setApprovedAt(LocalDateTime.now());

        LeaveResponse response = toResponse(leaveRequestRepository.save(req));
        mailService.sendLeaveApprovalResult(req.getUser().getEmail(), true, req.getLeaveType().getName());
        return response;
    }

    @Transactional
    public LeaveResponse reject(@NonNull Long requestId, @NonNull Long managerId) {
        LeaveRequest req = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("請假單不存在"));
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("此請假單已簽核，無法重複操作");
        }

        User manager = userRepository.getReferenceById(managerId);

        req.setStatus(RequestStatus.REJECTED);
        req.setApprovedBy(manager);
        req.setApprovedAt(LocalDateTime.now());

        LeaveResponse response = toResponse(leaveRequestRepository.save(req));
        mailService.sendLeaveApprovalResult(req.getUser().getEmail(), false, req.getLeaveType().getName());
        return response;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalance(@NonNull Long userId, int year) {
        int targetYear = year == 0 ? Year.now().getValue() : year;
        return leaveBalanceRepository.findByUserIdAndYear(userId, targetYear).stream()
                .map(this::toBalanceResponse)
                .toList();
    }

    private BigDecimal calculateLeaveDays(LocalDateTime start, LocalDateTime end) {
        long totalMinutes = java.time.Duration.between(start, end).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return hours.divide(HOURS_PER_DAY, 1, RoundingMode.HALF_UP);
    }

    private LeaveResponse toResponse(LeaveRequest req) {
        LeaveResponse resp = new LeaveResponse();
        resp.setId(req.getId());
        resp.setLeaveTypeName(req.getLeaveType().getName());
        resp.setLeaveTypeCode(req.getLeaveType().getCode());
        resp.setStartTime(req.getStartTime());
        resp.setEndTime(req.getEndTime());
        resp.setReason(req.getReason());
        resp.setAgentId(req.getAgent() != null ? req.getAgent().getId() : null);
        resp.setAgentName(req.getAgent() != null ? req.getAgent().getName() : null);
        resp.setStatus(req.getStatus().name());
        resp.setApprovedById(req.getApprovedBy() != null ? req.getApprovedBy().getId() : null);
        resp.setApprovedByName(req.getApprovedBy() != null ? req.getApprovedBy().getName() : null);
        resp.setApprovedAt(req.getApprovedAt());
        resp.setCreatedAt(req.getCreatedAt());
        return resp;
    }

    private LeaveBalanceResponse toBalanceResponse(LeaveBalance balance) {
        LeaveBalanceResponse resp = new LeaveBalanceResponse();
        resp.setLeaveTypeId(balance.getLeaveType().getId());
        resp.setLeaveTypeName(balance.getLeaveType().getName());
        resp.setLeaveTypeCode(balance.getLeaveType().getCode());
        resp.setYear(balance.getYear());
        resp.setTotalDays(balance.getTotalDays());
        resp.setUsedDays(balance.getUsedDays());
        resp.setRemainingDays(balance.getTotalDays().subtract(balance.getUsedDays()));
        return resp;
    }
}
