package com.ideaforge.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ideaforge.common.api.ErrorCode;
import com.ideaforge.common.exception.BizException;
import com.ideaforge.common.util.PasswordEncoder;
import com.ideaforge.user.dto.*;
import com.ideaforge.user.entity.User;
import com.ideaforge.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CODE_KEY = "user:code:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    public void sendCode(String phone) {
        String code = "123456";
        redisTemplate.opsForValue().set(CODE_KEY + phone, code, CODE_TTL);
        log.info("验证码已发送: phone={}, code={}", phone, code);
    }

    @Transactional
    public LoginResult register(RegisterReq req) {
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (existCount > 0) {
            throw new BizException(ErrorCode.USER_PHONE_EXISTS);
        }
        verifyCode(req.getPhone(), req.getCode());

        User user = new User();
        user.setUsername("user_" + req.getPhone().substring(req.getPhone().length() - 4));
        user.setPhone(req.getPhone());
        user.setNickname("IdeaForge 用户");
        user.setPasswordHash(PasswordEncoder.encode(req.getPassword()));
        userMapper.insert(user);

        return doLogin(user);
    }

    public LoginResult loginByPhone(PhoneLoginReq req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 0) {
            throw new BizException(ErrorCode.USER_ACCOUNT_DISABLED);
        }
        if (!PasswordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
        }
        return doLogin(user);
    }

    public UserProfileResp getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return toResp(user);
    }

    @Transactional
    public UserProfileResp updateProfile(Long userId, UpdateProfileReq req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        userMapper.updateById(user);
        return toResp(user);
    }

    public void logout() {
        StpUtil.logout();
    }

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
