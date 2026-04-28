package com.attendance.repository;

import com.attendance.entity.OvertimeRequest;
import com.attendance.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest, Long> {

    List<OvertimeRequest> findByUserId(Long userId);

    List<OvertimeRequest> findByStatusAndApprovedByIdIsNull(RequestStatus status);

    List<OvertimeRequest> findByStatusAndUser_ManagerId(RequestStatus status, Long managerId);
}
