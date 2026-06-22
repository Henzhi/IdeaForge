package com.ideaforge.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希工具。使用 BCrypt(cost=12),符合数据库设计文档安全要求。
 * 单例持有 encoder 避免重复实例化(BCrypt 每次实例化较重)。
 */
public final class PasswordEncoder {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private PasswordEncoder() {}

    /** 哈希明文密码 */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /** 校验明文与哈希是否匹配(恒定时间比较) */
    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) return false;
        return ENCODER.matches(rawPassword, hashedPassword);
    }
}
