package com.svtu.service.impl;

import com.svtu.common.Result;
import com.svtu.entity.User;
import com.svtu.entity.UserLogin;
import com.svtu.exception.UserException;
import com.svtu.mapper.UserRoleMapper;
import com.svtu.service.UserLoginService;
import com.svtu.util.JwtUtil;
import com.svtu.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class UserLoginServiceImpl implements UserLoginService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserRoleMapper userRoleMapper;

    /** 允许的连续登录失败次数，超过后锁定 */
    private static final int MAX_LOGIN_FAIL = 5;
    /** 失败计数与锁定时长（秒） */
    private static final long LOGIN_FAIL_LOCK_SECONDS = 600;

    //客户端和管理端登录接口不一样,客户端直接比对手机号和密码,管理端需要通过security来获取当前登录用户的角色
    @Override
    public Result<Object> AdminLogin(User user) {
        Map<String, String> map = Login("admin", user);
        return Result.success(map);
    }

    @Override
    public Result<Object> UserLogin(User user) {
        Map<String, String> map = Login("user", user);
        return Result.success(map);
    }

    public Map<String, String> Login(String type, User user){
        // 0. 校验失败次数，达到上限后直接拒绝，防止暴力破解
        String failKey = "login:fail:" + user.getPhone();
        String failValue = redisUtil.getString(failKey);
        if (failValue != null && Integer.parseInt(failValue) >= MAX_LOGIN_FAIL) {
            throw new UserException(501, "密码错误次数过多，请 10 分钟后再试");
        }

        // 1. 封装账号密码令牌
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(user.getPhone(), user.getPassword());
        // 2. 调用Security校验账号密码（共用UserDetailsServiceImpl）
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(token);
        } catch (Exception e) {
            // 校验失败：记录失败次数，连续失败达到上限后自动锁定
            redisUtil.incrWithExpire(failKey, LOGIN_FAIL_LOCK_SECONDS);
            throw new UserException(501, "账号或密码错误");
        }

        if (Objects.isNull(authentication)) {
            throw new UserException(501, "登录失败，账号或密码错误");
        }
        // 登录成功：清除失败计数
        redisUtil.deleteString(failKey);
        Map<String, String> resultMap = new HashMap<>();
        // 3. 获取登录用户主体
        UserLogin userLogin = (UserLogin) authentication.getPrincipal();

        // 查询角色列表，存入 userLogin 供拦截器使用（所有登录类型都查）
        List<String> roleList = userRoleMapper.selectUserRoleName(userLogin.getUser().getUserId());
        userLogin.setRoles(roleList != null ? roleList : Collections.emptyList());

        if ("admin".equals(type)){
            // 管理端登录：无角色（普通用户）禁止登录
            if (roleList == null || roleList.isEmpty()) {
                throw new AccessDeniedException("普通用户禁止登录管理后台");
            }
            resultMap.put("role", roleList.get(0));
        }

        // 4. 生成JWT令牌
        String jwt = jwtUtil.generateToken(userLogin, userLogin.getUser().getUserId());
        resultMap.put("token", jwt);

        // 5. 存入Redis缓存登录信息（过期时间与 JWT 一致，避免会话缓存永久堆积）
        String redisKey = "login:" + userLogin.getUser().getUserId();
        redisUtil.set(redisKey, userLogin, jwtUtil.getExpireSeconds());

        return resultMap;
    }

    @Override
    public void logout(Integer userId) {
        // 删除服务端会话缓存，此后携带旧令牌的请求会被拦截器拒绝
        redisUtil.delete("login:" + userId);
    }
}
