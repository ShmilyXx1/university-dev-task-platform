package com.svtu.exception;

import com.svtu.common.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获自定义业务异常 UserException
    @ExceptionHandler(UserException.class)
    public Result<?> handleUserException(UserException e) {
        Result<?> result = new Result<>();
        result.setCode(e.getCode());
        result.setMsg(e.getMessage());
        result.setData(null);
        return result;
    }

    /**
     * 请求体格式不支持（最常见：@RequestBody 只收 JSON，调用方却用了 x-www-form-urlencoded 表单提交）
     * 打印具体请求方法、路径和 Content-Type，方便立刻定位是哪个调用方协议不匹配
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<?> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        System.err.println("[415 不支持的媒体类型] " + request.getMethod() + " " + request.getRequestURI()
                + " | Content-Type=" + request.getContentType()
                + " | 该接口需要 application/json，请勿使用 x-www-form-urlencoded 表单提交");
        Result<?> result = new Result<>();
        result.setCode(415);
        result.setMsg("请求格式不支持：请使用 application/json 提交数据（当前 Content-Type: " + e.getContentType() + "）");
        result.setData(null);
        return result;
    }
    // 兜底捕获所有系统异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) throws Exception {
        // ✅ AccessDeniedException 不要重新throw,直接返回 Result,前端才能拿到正确的msg
        if (e instanceof AccessDeniedException) {
            String msg = e.getMessage();
            if (msg == null || msg.trim().isEmpty()) {
                msg = "用户没有该权限";
            }
            return Result.error(msg);
        }
        if (e instanceof AuthenticationEntryPoint) {
            throw e;
        }
        e.printStackTrace(); // 顺便打日志,方便排查真正的"系统异常"
        return Result.error("系统异常");
    }

//    // 兜底捕获所有系统异常
//    @ExceptionHandler(Exception.class)
//    public Result<?> handleException(Exception e) throws Exception {
//        // Spring Security权限/登录异常交给框架原生处理
//        if (e instanceof AccessDeniedException) {
//            throw e;
//        }
//        if (e instanceof AuthenticationEntryPoint) {
//            throw e;
//        }
//        // 其余系统异常统一返回
//        return Result.error("系统异常");
//    }
    // 捕获AdminException
    @ExceptionHandler(AdminException.class)
    public Result<?> handleAdminException(AdminException e) {
        Result<?> result = new Result<>();
        result.setCode(e.getCode());
        result.setMsg(e.getMessage());
        result.setData(null);
        return result;
    }
}