package com.attendance.service;

import com.attendance.dto.user.*;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.entity.UserRole;
import com.attendance.repository.DepartmentRepository;
import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest extends ServiceTestBase {

    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MailService mailService;

    @InjectMocks private UserService userService;

    private Department itDept;
    private User adminUser;
    private User managerUser;
    private User employeeUser;

    @BeforeEach
    void setUp() {
        itDept = new Department(1L, "資訊部", null);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@company.com");
        adminUser.setName("系統管理員");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setDepartment(itDept);
        adminUser.setIsActive(true);
        adminUser.setMustChangePassword(false);

        managerUser = new User();
        managerUser.setId(2L);
        managerUser.setEmail("manager@company.com");
        managerUser.setName("王小明");
        managerUser.setRole(UserRole.MANAGER);
        managerUser.setDepartment(itDept);
        managerUser.setManager(adminUser);
        managerUser.setIsActive(true);
        managerUser.setMustChangePassword(false);

        employeeUser = new User();
        employeeUser.setId(3L);
        employeeUser.setEmail("employee@company.com");
        employeeUser.setName("李小華");
        employeeUser.setRole(UserRole.EMPLOYEE);
        employeeUser.setDepartment(itDept);
        employeeUser.setManager(managerUser);
        employeeUser.setIsActive(true);
        employeeUser.setMustChangePassword(false);
    }

    // ─── listUsers ───────────────────────────────────────────

    @Nested
    @DisplayName("listUsers()")
    class ListUsersTests {

        @Test
        @DisplayName("無搜尋條件 — 回傳分頁結果")
        void listAll() {
            Page<User> page = new PageImpl<>(List.of(adminUser, managerUser, employeeUser));
            when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

            UserListResponse resp = userService.listUsers(0, 20, null);

            assertThat(resp.getContent()).hasSize(3);
            assertThat(resp.getTotalElements()).isEqualTo(3);
            assertThat(resp.getPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("有搜尋條件 — 依姓名/Email 搜尋")
        void listWithSearch() {
            Page<User> page = new PageImpl<>(List.of(adminUser));
            when(userRepository.findByNameContainingOrEmailContaining(
                    eq("管理"), eq("管理"), any(Pageable.class))).thenReturn(page);

            UserListResponse resp = userService.listUsers(0, 20, "管理");

            assertThat(resp.getContent()).hasSize(1);
            assertThat(resp.getContent().get(0).getName()).isEqualTo("系統管理員");
        }

        @Test
        @DisplayName("空白搜尋 — 視為無搜尋條件")
        void listWithBlankSearch() {
            Page<User> page = new PageImpl<>(List.of(adminUser));
            when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

            UserListResponse resp = userService.listUsers(0, 20, "   ");

            verify(userRepository).findAll(any(Pageable.class));
            verify(userRepository, never()).findByNameContainingOrEmailContaining(anyString(), anyString(), any());
        }
    }

    // ─── getUser ──────────────────────────────────────────────

    @Nested
    @DisplayName("getUser()")
    class GetUserTests {

        @Test
        @DisplayName("成功取得使用者")
        void getUserSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            UserResponse resp = userService.getUser(1L);

            assertThat(resp.getId()).isEqualTo(1L);
            assertThat(resp.getEmail()).isEqualTo("admin@company.com");
            assertThat(resp.getDeptName()).isEqualTo("資訊部");
        }

        @Test
        @DisplayName("使用者不存在 — 拋出異常")
        void getUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("使用者不存在");
        }
    }

    // ─── createUser ───────────────────────────────────────────

    @Nested
    @DisplayName("createUser()")
    class CreateUserTests {

        @Test
        @DisplayName("成功建立使用者 — 密碼加密 + 發送 Email")
        void createUserSuccess() {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("new@company.com");
            req.setName("新員工");
            req.setRole("EMPLOYEE");
            req.setDeptId(1L);
            req.setManagerId(2L);

            when(userRepository.findByEmail("new@company.com")).thenReturn(Optional.empty());
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));
            when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(4L);
                return u;
            });

            UserResponse resp = userService.createUser(req);

            assertThat(resp.getEmail()).isEqualTo("new@company.com");
            assertThat(resp.getName()).isEqualTo("新員工");
            verify(passwordEncoder).encode(anyString());
            verify(mailService).sendNewUserCredentials(eq("new@company.com"), eq("新員工"), anyString());
        }

        @Test
        @DisplayName("Email 重複 — 拋出異常")
        void createUserDuplicateEmail() {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("admin@company.com");
            req.setName("重複");
            req.setRole("EMPLOYEE");

            when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email 已被使用");

            verify(mailService, never()).sendNewUserCredentials(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("部門不存在 — 拋出異常")
        void createUserDeptNotFound() {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("new@company.com");
            req.setName("新員工");
            req.setRole("EMPLOYEE");
            req.setDeptId(999L);

            when(userRepository.findByEmail("new@company.com")).thenReturn(Optional.empty());
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("部門不存在");
        }

        @Test
        @DisplayName("mustChangePassword 預設為 true")
        void createUserMustChangePassword() {
            UserCreateRequest req = new UserCreateRequest();
            req.setEmail("new@company.com");
            req.setName("新員工");
            req.setRole("EMPLOYEE");

            when(userRepository.findByEmail("new@company.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(4L);
                return u;
            });

            UserResponse resp = userService.createUser(req);

            verify(userRepository).save(argThat(user ->
                    user.getMustChangePassword() && user.getIsActive()));
        }
    }

    // ─── updateUser ───────────────────────────────────────────

    @Nested
    @DisplayName("updateUser()")
    class UpdateUserTests {

        @Test
        @DisplayName("成功更新姓名和角色")
        void updateNameAndRole() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setName("更新名稱");
            req.setRole("MANAGER");

            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeUser));
            when(userRepository.save(any(User.class))).thenReturn(employeeUser);

            UserResponse resp = userService.updateUser(3L, req);

            assertThat(resp.getName()).isEqualTo("更新名稱");
            assertThat(resp.getRole()).isEqualTo("MANAGER");
        }

        @Test
        @DisplayName("更新部門和主管")
        void updateDeptAndManager() {
            Department hrDept = new Department(2L, "人事部", null);
            UserUpdateRequest req = new UserUpdateRequest();
            req.setDeptId(2L);
            req.setManagerId(1L);

            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeUser));
            when(departmentRepository.findById(2L)).thenReturn(Optional.of(hrDept));
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
            when(userRepository.save(any(User.class))).thenReturn(employeeUser);

            UserResponse resp = userService.updateUser(3L, req);

            verify(userRepository).save(argThat(user ->
                    user.getDepartment().getId().equals(2L) &&
                    user.getManager().getId().equals(1L)));
        }

        @Test
        @DisplayName("使用者不存在 — 拋出異常")
        void updateUserNotFound() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setName("更新");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(999L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("使用者不存在");
        }
    }

    // ─── deactivateUser ───────────────────────────────────────

    @Nested
    @DisplayName("deactivateUser()")
    class DeactivateUserTests {

        @Test
        @DisplayName("成功停用使用者 — isActive 設為 false")
        void deactivateSuccess() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeUser));
            when(userRepository.save(any(User.class))).thenReturn(employeeUser);

            userService.deactivateUser(3L);

            verify(userRepository).save(argThat(user -> !user.getIsActive()));
        }

        @Test
        @DisplayName("使用者不存在 — 拋出異常")
        void deactivateNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deactivateUser(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("使用者不存在");
        }
    }
}
