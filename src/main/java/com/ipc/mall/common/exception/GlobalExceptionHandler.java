package com.ipc.mall.common.exception;

import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：把异常统一转成 Result，避免控制台 500 堆栈直接甩给用户
 * <p>
 * 后续如果某个页面希望异常时跳错误页而不是返回 JSON，
 * 可以再加一个 @ControllerAdvice 配合 ModelAndView 单独处理页面请求。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：属于预期内的问题，只记 warn，不打印堆栈 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @Valid 校验失败（RequestBody 方式） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        log.warn("参数校验失败: {}", message);
        return Result.fail(ResultCode.FAIL.getCode(), message);
    }

    /** @Valid 校验失败（表单方式） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        log.warn("参数绑定失败: {}", message);
        return Result.fail(ResultCode.FAIL.getCode(), message);
    }

    /** 上传超过 Spring 限制（5MB）：按业务码 4001 返回，不要把 500 甩给页面 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制: {}", e.getMessage());
        return Result.fail(ResultCode.UPLOAD_INVALID);
    }

    /**
     * 静态资源 / 页面地址不存在：保持 404 语义，不要被下面的兜底处理器包装成 200 JSON
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /** 兜底：未预料的异常，完整打印堆栈方便排查 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.ERROR.getCode(), "服务器开小差了，请稍后再试");
    }
}
