package com.ipc.mall.common.exception;

import com.ipc.mall.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：Service 层校验不通过时直接抛出，由全局异常处理器统一转成 Result
 * <p>
 * 用法：throw new BizException("库存不足");
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
