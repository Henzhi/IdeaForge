package com.ideaforge.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileReq {

    @Size(max = 100)
    private String nickname;

    @Size(max = 500)
    private String avatarUrl;
}
