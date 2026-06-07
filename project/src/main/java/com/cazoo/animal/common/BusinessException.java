package com.cazoo.animal.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ResultCode code;

    public BusinessException(ResultCode code) {
        super(code.getMsg());
        this.code = code;
    }

    public BusinessException(ResultCode code, String msg) {
        super(msg);
        this.code = code;
    }
}
