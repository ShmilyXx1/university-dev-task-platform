package com.svtu.handler;

import com.alibaba.fastjson.JSON;
import com.svtu.common.Result;
import com.svtu.util.WebUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 已登录但无权限访问（@PreAuthorize 校验失败）
 * HTTP 403 Forbidden —— 前端不要清除 token，只提示"无权限"
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = new Result<>(403, "无权限访问，请联系管理员");
        response.getWriter().write(JSON.toJSONString(result));
    }
}
