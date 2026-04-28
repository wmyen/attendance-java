package com.attendance.service;

import com.attendance.dto.user.*;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.entity.UserRole;
import com.attendance.repository.DepartmentRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 12;

    @Transactional(readOnly = true)
    public UserListResponse listUsers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<User> userPage;

        if (search != null && !search.isBlank()) {
            userPage = userRepository.findByNameContainingOrEmailContaining(search, search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return new UserListResponse(
                userPage.getContent().stream().map(this::toResponse).toList(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email 已被使用");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setRole(UserRole.valueOf(request.getRole()));
        user.setDepartment(resolveDepartment(request.getDeptId()));
        user.setManager(resolveUser(request.getManagerId()));
        user.setAgent(resolveUser(request.getAgentId()));
        user.setMustChangePassword(true);
        user.setIsActive(true);

        String rawPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));

        User saved = userRepository.save(user);
        mailService.sendNewUserCredentials(saved.getEmail(), saved.getName(), rawPassword);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findById(id);

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getRole() != null) {
            user.setRole(UserRole.valueOf(request.getRole()));
        }
        if (request.getDeptId() != null) {
            user.setDepartment(resolveDepartment(request.getDeptId()));
        }
        if (request.getManagerId() != null) {
            user.setManager(resolveUser(request.getManagerId()));
        }
        if (request.getAgentId() != null) {
            user.setAgent(resolveUser(request.getAgentId()));
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = findById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));
    }

    private Department resolveDepartment(Long deptId) {
        if (deptId == null) return null;
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new IllegalArgumentException("部門不存在"));
    }

    private User resolveUser(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("指定的使用者不存在"));
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        return random.ints(PASSWORD_LENGTH, 0, CHAR_POOL.length())
                .mapToObj(CHAR_POOL::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : null,
                user.getManager() != null ? user.getManager().getId() : null,
                user.getManager() != null ? user.getManager().getName() : null,
                user.getAgent() != null ? user.getAgent().getId() : null,
                user.getAgent() != null ? user.getAgent().getName() : null,
                user.getMustChangePassword(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
