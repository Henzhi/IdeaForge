package com.ideaforge.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ideaforge.common.api.Result;
import com.ideaforge.user.dto.*;
import com.ideaforge.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/send-code")
    public Result<?> sendCode(@RequestParam @NotBlank String phone) {
        userService.sendCode(phone);
        return Result.ok("验证码已发送");
    }

    @PostMapping("/register")
    public Result<LoginResult> register(@RequestBody @Valid RegisterReq req) {
        return Result.ok(userService.register(req));
    }

    @PostMapping("/login/phone")
    public Result<LoginResult> loginByPhone(@RequestBody @Valid PhoneLoginReq req) {
        return Result.ok(userService.loginByPhone(req));
    }

    @GetMapping("/profile")
    public Result<UserProfileResp> profile() {
        return Result.ok(userService.getProfile(StpUtil.getLoginIdAsLong()));
    }

    @PutMapping("/profile")
    public Result<UserProfileResp> updateProfile(@RequestBody @Valid UpdateProfileReq req) {
        return Result.ok(userService.updateProfile(StpUtil.getLoginIdAsLong(), req));
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        userService.logout();
        return Result.ok();
    }
}
