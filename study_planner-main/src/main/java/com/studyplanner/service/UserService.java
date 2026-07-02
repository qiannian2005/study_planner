package com.studyplanner.service;

import com.studyplanner.dto.LoginRequest;
import com.studyplanner.dto.RegisterRequest;
import com.studyplanner.entity.User;
import com.studyplanner.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final String ROLE_ADMIN = "admin";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_DISABLED = "disabled";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileUploadService fileUploadService;

    @PostConstruct
    public void initializeSchema() {
        if (!columnExists("user", "role")) {
            userMapper.addRoleColumn();
        }
        if (!columnExists("user", "status")) {
            userMapper.addStatusColumn();
        }
    }

    public User register(RegisterRequest request) {
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        if (request.getEmail() != null && userMapper.findByEmail(request.getEmail()) != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        userMapper.insert(user);

        try {
            String avatarUrl = fileUploadService.generateDefaultAvatar(user.getId(), user.getUsername());
            userMapper.updateAvatar(user.getId(), avatarUrl);
            user.setAvatar(avatarUrl);
        } catch (Exception ignored) {
            // 头像生成失败不阻塞注册流程，用户可后续手动上传
        }

        user.setRole("user");
        user.setStatus(STATUS_ACTIVE);
        user.setPassword(null);
        return user;
    }

    public User login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (STATUS_DISABLED.equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        user.setPassword(null);
        return user;
    }

    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public User updateUser(User user) {
        userMapper.update(user);
        return getUserById(user.getId());
    }

    public User updateAvatar(Long userId, String avatarUrl) {
        userMapper.updateAvatar(userId, avatarUrl);
        return getUserById(userId);
    }

    public User updateProfile(Long userId, String email) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (email != null && !email.equals(user.getEmail())) {
            User existingUser = userMapper.findByEmail(email);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new RuntimeException("该邮箱已被其他用户使用");
            }
        }

        user.setEmail(email);
        userMapper.update(user);
        return getUserById(userId);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    public boolean isAdmin(Long userId) {
        User user = userMapper.findById(userId);
        return user != null && ROLE_ADMIN.equals(user.getRole()) && !STATUS_DISABLED.equals(user.getStatus());
    }

    public List<Map<String, Object>> getAdminUsers(String keyword) {
        return userMapper.findAdminUsers(keyword);
    }

    public void updateUserStatus(Long userId, String status) {
        if (!STATUS_ACTIVE.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new IllegalArgumentException("Invalid user status");
        }
        userMapper.updateStatus(userId, status);
    }

    public void resetUserPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword.trim()));
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
