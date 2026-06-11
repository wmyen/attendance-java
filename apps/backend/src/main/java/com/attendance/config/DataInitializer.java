package com.attendance.config;

import com.attendance.entity.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 開發環境種子資料初始化器。
 * 啟動時自動建立測試使用者、假別餘額（僅在資料為空時執行）。
 *
 * 種子帳號（密碼均為 Admin@2026，首次登入需改密碼）：
 *   - admin@company.com    (ADMIN)
 *   - manager@company.com  (MANAGER, 隸屬資訊部, 主管為 admin)
 *   - employee@company.com (EMPLOYEE, 隸屬資訊部, 主管為 manager)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "Admin@2026";
    private static final int CURRENT_YEAR = java.time.Year.now().getValue();

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("種子資料已存在，跳過初始化");
            return;
        }

        log.info("=== 開始初始化種子資料 ===");

        // 取得部門（由 data.sql 建立）
        Department hrDept = departmentRepository.findByName("人事部").orElse(null);
        Department itDept = departmentRepository.findByName("資訊部").orElse(null);

        // 取得假別（由 data.sql 建立）
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();

        // 建立使用者
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        User admin = createUser("admin@company.com", "系統管理員", UserRole.ADMIN,
                hrDept, null, null, encodedPassword);
        User manager = createUser("manager@company.com", "王小明", UserRole.MANAGER,
                itDept, admin, null, encodedPassword);
        User employee = createUser("employee@company.com", "李小華", UserRole.EMPLOYEE,
                itDept, manager, null, encodedPassword);

        log.info("已建立 3 個種子使用者（密碼: {}）", DEFAULT_PASSWORD);

        // 建立假別餘額
        for (LeaveType lt : leaveTypes) {
            createBalance(admin, lt);
            createBalance(manager, lt);
            createBalance(employee, lt);
        }

        log.info("已建立假別餘額（{} 年度，{} 個使用者 × {} 個假別）",
                CURRENT_YEAR, 3, leaveTypes.size());
        log.info("=== 種子資料初始化完成 ===");
    }

    private User createUser(String email, String name, UserRole role,
                            Department dept, User manager, User agent,
                            String encodedPassword) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setDepartment(dept);
        user.setManager(manager);
        user.setAgent(agent);
        user.setPassword(encodedPassword);
        user.setMustChangePassword(true);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private void createBalance(User user, LeaveType leaveType) {
        BigDecimal totalDays = switch (leaveType.getCode()) {
            case "ANNUAL" -> user.getRole() == UserRole.EMPLOYEE ? new BigDecimal("7.0") : new BigDecimal("14.0");
            case "SICK" -> new BigDecimal("30.0");
            case "PERSONAL" -> new BigDecimal("14.0");
            default -> BigDecimal.ZERO; // 補休、婚假等初始為 0
        };

        LeaveBalance balance = new LeaveBalance();
        balance.setUser(user);
        balance.setLeaveType(leaveType);
        balance.setYear(CURRENT_YEAR);
        balance.setTotalDays(totalDays);
        balance.setUsedDays(BigDecimal.ZERO);
        leaveBalanceRepository.save(balance);
    }
}
