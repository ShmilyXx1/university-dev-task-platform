package com.svtu.service.impl;

import com.svtu.common.Result;
import com.svtu.entity.User;
import com.svtu.entity.UserLogin;
import com.svtu.exception.UserException;
import com.svtu.mapper.AdminMenuMapper;
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
    private AdminMenuMapper adminMenuMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;

    //客户端和管理端登录接口不一样,需要再写一个登录接口,客户端中只需要直接在数据库中比对手机号和密码就行,而管理端需要通过security来获取当前登录用户的权限
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

    public  Map<String, String> Login(String type,User user){
        // 1. 封装账号密码令牌
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(user.getPhone(), user.getPassword());
        // 2. 调用Security校验账号密码（共用UserDetailsServiceImpl）
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(token);
        } catch (Exception e) {
            // 账号或密码错误时，Spring Security 会抛 BadCredentialsException 等异常
            throw new UserException(501, "账号或密码错误");
        }

        if (Objects.isNull(authentication)) {
            throw new UserException(501, "登录失败，账号或密码错误");
        }
        Map<String, String> resultMap = new HashMap<>();
        // 3. 获取登录用户主体
        UserLogin userLogin = (UserLogin) authentication.getPrincipal();
        if ("admin".equals(type)){
            List<String> list = adminMenuMapper.selectUserAllMenu(userLogin.getUser().getUserId());
            System.out.println(list);
            if (list==null||list.isEmpty()){ //查询用户所有菜单权限,普通用户没有任何菜单权限,所有list会为空
                throw new AccessDeniedException("普通用户禁止登录管理后台");
            }
            List<String> list1=userRoleMapper.selectUserRoleName(userLogin.getUser().getUserId());
            String position=list1.get(0);
            resultMap.put("role",position);
            userLogin.setPermissions(list);
        }

        // 4. 生成JWT令牌
        String jwt = jwtUtil.generateToken(userLogin, userLogin.getUser().getUserId());
        resultMap.put("token", jwt);


        // 5. 存入Redis缓存登录信息
        String redisKey = "login:" + userLogin.getUser().getUserId();
        redisUtil.setToken(redisKey, userLogin);

        return resultMap;
    }
}
