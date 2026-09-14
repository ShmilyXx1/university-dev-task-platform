package com.svtu.handler;

import com.alibaba.fastjson.JSON;
import com.svtu.common.Result;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 未登录 / token 无效（Spring Security 过滤器链阶段抛出）
 * HTTP 401 Unauthorized —— 前端清除 token，跳登录
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = new Result<>(401, "未登录或登录已过期，请重新登录");
        response.getWriter().write(JSON.toJSONString(result));
    }
}
