package com.health.framework.exception;

import com.health.common.core.AjaxResult;
import com.health.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e) {
        log.error("业务异常：{}", e.getMessage(), e);
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public AjaxResult handleNotLogin(NotLoginException e) {
        return AjaxResult.error(401, "请先登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    public AjaxResult handleNoPerm(NotPermissionException e) {
        return AjaxResult.error(403, "无操作权限");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleValidation(MethodArgumentNotValidException e) {
        var fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : "参数校验失败";
        return AjaxResult.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return AjaxResult.error("系统内部异常");
    }
}
