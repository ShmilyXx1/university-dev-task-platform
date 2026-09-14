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


@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Result<Object> userResult= Result.error("用户没有该权限");
        String json= JSON.toJSONString(userResult);

        //处理异常
        WebUtils.writeJson(response,json);
    }
}
