package com.ideaforge.common.exception;

import com.ideaforge.common.api.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。Service 层抛出,由 GlobalExceptionHandler 统一捕获转 Result。
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(detail);
        this.code = errorCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
