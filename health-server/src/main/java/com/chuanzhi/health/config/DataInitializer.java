package com.chuanzhi.health.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanzhi.health.entity.User;
import com.chuanzhi.health.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")
        );
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setRole("ADMIN");
            admin.setName("系统管理员");
            admin.setGender(1);
            admin.setAge(30);
            admin.setPhone("13800000000");
            admin.setEmail("admin@health.com");
            admin.setBloodType("A");
            admin.setPassword(passwordEncoder.encode("admin123"));
            userMapper.insert(admin);
            log.info("管理员账号已创建: admin / admin123");
        } else {
            // 确保密码是最新的 BCrypt 编码
            String encoded = passwordEncoder.encode("admin123");
            if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                admin.setPassword(encoded);
                userMapper.updateById(admin);
                log.info("管理员密码已更新");
            }
        }
    }
}
