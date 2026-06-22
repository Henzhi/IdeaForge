package com.ideaforge.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterReq {

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank
    @Size(min = 4, max = 6, message = "验证码长度须为4-6位")
    private String code;

    @NotBlank
    @Size(min = 6, max = 64, message = "密码长度须为6-64位")
    private String password;
}
