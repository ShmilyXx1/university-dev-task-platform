package com.svtu.handler;

import com.alibaba.fastjson.JSON;
import com.svtu.common.Result;
import com.svtu.util.WebUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        Result<Object> userResult = new Result<>(401,"用户认证失败，请重新登录");
        String json= JSON.toJSONString(userResult);

        //处理异常
        WebUtils.writeJson(response,json);
    }
}
