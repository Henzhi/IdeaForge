package com.ideaforge.user.dto;

import lombok.Data;

@Data
public class UserProfileResp {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String email;
}
