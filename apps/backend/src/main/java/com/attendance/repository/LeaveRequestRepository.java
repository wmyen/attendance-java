package com.attendance.repository;

import com.attendance.entity.LeaveRequest;
import com.attendance.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByUserId(Long userId);

    List<LeaveRequest> findByStatusAndApprovedByIdIsNull(RequestStatus status);

    List<LeaveRequest> findByStatusAndUser_ManagerId(RequestStatus status, Long managerId);
}
