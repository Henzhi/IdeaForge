package com.ideaforge.common.api;

import lombok.Getter;

/**
 * 业务错误码枚举。统一管理所有可预期的错误返回。
 */
@Getter
public enum ErrorCode {
    // 通用 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或会话已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    // 业务 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_PHONE_EXISTS(1003, "手机号已注册"),
    USER_CODE_INVALID(1004, "验证码错误或已过期"),
    USER_ACCOUNT_DISABLED(1005, "账号已被禁用"),

    IDEA_NOT_FOUND(1101, "想法不存在"),
    IDEA_PERMISSION_DENIED(1102, "无权操作该想法"),

    STORY_NOT_FOUND(1201, "故事不存在"),
    STORY_GENERATION_LIMIT(1202, "今日生成次数已达上限"),
    STORY_GENERATION_FAILED(1203, "故事生成失败"),

    // 系统 5xxx
    INTERNAL_ERROR(5000, "服务器内部错误"),
    LLM_UNAVAILABLE(5001, "AI 服务暂不可用,请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
