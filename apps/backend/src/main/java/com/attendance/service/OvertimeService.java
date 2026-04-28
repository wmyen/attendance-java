package com.attendance.service;

import com.attendance.dto.overtime.OvertimeApplyRequest;
import com.attendance.dto.overtime.OvertimeResponse;
import com.attendance.entity.OvertimeRequest;
import com.attendance.entity.RequestStatus;
import com.attendance.entity.User;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.OvertimeRequestRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeRequestRepository overtimeRequestRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Transactional
    public OvertimeResponse apply(Long userId, OvertimeApplyRequest request) {
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
    public List<OvertimeResponse> getMyOvertimes(Long userId) {
        return overtimeRequestRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OvertimeResponse> getPendingOvertimes(Long managerId) {
        return overtimeRequestRepository.findByStatusAndUser_ManagerId(RequestStatus.PENDING, managerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OvertimeResponse approve(Long requestId, Long managerId) {
        OvertimeRequest req = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("加班申請不存在"));
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("此加班申請已簽核，無法重複操作");
        }

        User manager = userRepository.getReferenceById(managerId);

        req.setStatus(RequestStatus.APPROVED);
        req.setApprovedBy(manager);
        req.setApprovedAt(LocalDateTime.now());

        OvertimeResponse response = toResponse(overtimeRequestRepository.save(req));
        mailService.sendOvertimeApprovalResult(req.getUser().getEmail(), true);
        return response;
    }

    @Transactional
    public OvertimeResponse reject(Long requestId, Long managerId) {
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
