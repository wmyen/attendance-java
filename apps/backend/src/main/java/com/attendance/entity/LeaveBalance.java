package com.attendance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.lang.NonNull;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "leave_type_id", "year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    public Long getId() { return Objects.requireNonNull(id); }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "total_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalDays;

    @Column(name = "used_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal usedDays = BigDecimal.ZERO;
}
