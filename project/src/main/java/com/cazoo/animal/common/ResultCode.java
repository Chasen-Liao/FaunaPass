package com.cazoo.animal.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(0, "success"),
    USERNAME_EXISTS(1001, "用户名已存在"),
    INVALID_CREDENTIALS(1002, "用户名或密码错误"),
    USER_NOT_FOUND(1003, "用户不存在"),
    ANIMAL_NOT_FOUND(2001, "动物档案不存在"),
    FILE_TYPE_INVALID(3001, "不支持的图片格式"),
    FILE_TOO_LARGE(3002, "图片大小超过 5MB"),
    VALIDATION_FAILED(4001, "参数校验失败"),
    UNAUTHORIZED(4002, "未登录或 token 失效"),
    ACCESS_DENIED(4003, "无权限访问"),
    INTERNAL_ERROR(9999, "服务器内部错误");

    private final int code;
    private final String msg;
}
