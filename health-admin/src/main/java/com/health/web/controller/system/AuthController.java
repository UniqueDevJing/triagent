package com.health.web.controller.system;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysUser;
import com.health.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    public AuthController(SysUserMapper userMapper,
                          StringRedisTemplate redisTemplate,
                          PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/captchaImage")
    public AjaxResult captcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 80, 4, 50);
        String captchaKey = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("captcha:" + captchaKey, captcha.getCode(), 2, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", captcha.getImageBase64Data());
        return AjaxResult.success(result);
    }

    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaKey = body.get("captchaKey");
        String captchaCode = body.get("captchaCode");

        // 验证码校验
        if (captchaKey == null || captchaCode == null) {
            return AjaxResult.error("请输入验证码");
        }
        String redisCode = redisTemplate.opsForValue().get("captcha:" + captchaKey);
        if (redisCode == null) {
            return AjaxResult.error("验证码已过期，请刷新");
        }
        if (!captchaCode.equalsIgnoreCase(redisCode)) {
            return AjaxResult.error("验证码错误");
        }
        redisTemplate.delete("captcha:" + captchaKey);

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, username));

        if (user == null) {
            return AjaxResult.error("用户名或密码错误");
        }

        if (!"0".equals(user.getStatus())) {
            return AjaxResult.error("账号已停用，请联系管理员");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return AjaxResult.error("用户名或密码错误");
        }

        // 清空密码，双保险配合 @JsonIgnore
        user.setPassword(null);

        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenInfo.getTokenValue());
        result.put("user", user);
        return AjaxResult.success(result);
    }

    @PostMapping("/logout")
    public AjaxResult logout() {
        StpUtil.logout();
        return AjaxResult.success();
    }
}
