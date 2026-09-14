package com.svtu.interceptor;

import com.svtu.entity.UserLogin;
import com.svtu.exception.UserException;
import com.svtu.util.JwtUtil;
import com.svtu.util.RedisUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class LoginInterceptor extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtils;

    @Autowired
    private RedisUtil redisUtil;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/user/userLogin",
            "/user/adminLogin",
            "/user/register",
            "/user/registerGetCode",
            "/user/registerCheckCode",
            "/user/updatePassword",
            "/user/forgetPasswordGetCode",
            "/user/forgetPasswordCheckCode",
            "/ws/chat"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 1. 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 2. 公开接口直接放行
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 3. 验证 token
        String token = request.getHeader("token");
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("未登录或 token 无效");
            return;
        }

        try {
            Claims claims = jwtUtils.extractAllClaims(token);
            Integer userId = (Integer) claims.get("userId");
            UserLogin userLogin = (UserLogin) redisUtil.get("login:" + userId);
            if (userLogin == null) {
                throw new UserException(501, "请先登录");
            }

            // 4. 从缓存中的 UserLogin 读取角色，构建 authorities 供 @PreAuthorize 使用
            List<String> roles = userLogin.getRoles();
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }

            // 将认证信息（含角色）存入 SecurityContext
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userLogin.getUser().getUserId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            // 放行请求，继续执行后续过滤器链
            chain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(401);
            response.getWriter().write("token 验证失败：" + e.getMessage());
        }
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (publicPath.equals(path) || path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
}
