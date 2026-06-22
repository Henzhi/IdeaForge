package com.ideaforge.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.ErrorCode;
import com.ideaforge.common.exception.BizException;
import com.ideaforge.common.util.PasswordEncoder;
import com.ideaforge.user.dto.*;
import com.ideaforge.user.entity.User;
import com.ideaforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 用户服务。含注册、登录(手机号+密码)、资料维护。
 * 验证码当前以 Redis 存储演示(dev 固定 123456),生产应接入短信网关。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String CODE_KEY = "user:code:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    /** 发送验证码 */
    public void sendCode(String phone) {
        // TODO: 接入真实短信网关。dev 环境固定返回 123456。
        String code = "123456";
        redisTemplate.opsForValue().set(CODE_KEY + phone, code, CODE_TTL);
        log.info("验证码已发送: phone={}, code={}", phone, code);
    }

    /** 手机号注册 */
    @Transactional
    public LoginResult register(RegisterReq req) {
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BizException(ErrorCode.USER_PHONE_EXISTS);
        }
        verifyCode(req.getPhone(), req.getCode());

        User user = new User();
        user.setUsername("user_" + req.getPhone().substring(req.getPhone().length() - 4));
        user.setPhone(req.getPhone());
        user.setNickname("IdeaForge 用户");
        user.setPasswordHash(PasswordEncoder.encode(req.getPassword()));
        user = userRepository.save(user);

        return doLogin(user);
    }

    /** 手机号+密码登录 */
    public LoginResult loginByPhone(PhoneLoginReq req) {
        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == 0) {
            throw new BizException(ErrorCode.USER_ACCOUNT_DISABLED);
        }
        if (!PasswordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
        }
        return doLogin(user);
    }

    /** 获取当前用户资料 */
    public UserProfileResp getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        return toResp(user);
    }

    /** 修改当前用户资料 */
    @Transactional
    public UserProfileResp updateProfile(Long userId, UpdateProfileReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        userRepository.save(user);
        return toResp(user);
    }

    /** 退出登录 */
    public void logout() {
        StpUtil.logout();
    }

    // ===== 内部方法 =====

    private void verifyCode(String phone, String code) {
        String cached = redisTemplate.opsForValue().get(CODE_KEY + phone);
        if (cached == null || !cached.equals(code)) {
            throw new BizException(ErrorCode.USER_CODE_INVALID);
        }
        redisTemplate.delete(CODE_KEY + phone);
    }

    private LoginResult doLogin(User user) {
        StpUtil.login(user.getId());
        return new LoginResult(StpUtil.getTokenValue(), toResp(user));
    }

    private UserProfileResp toResp(User user) {
        UserProfileResp resp = new UserProfileResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setPhone(maskPhone(user.getPhone()));
        resp.setEmail(user.getEmail());
        return resp;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
