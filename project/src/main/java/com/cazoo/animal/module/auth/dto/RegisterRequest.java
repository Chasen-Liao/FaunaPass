package com.cazoo.animal.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50")
    private String username;
    @NotBlank
    @Size(min = 6, max = 50, message = "密码长度必须在 6-50")
    private String password;
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50)
    private String nickname;
}
