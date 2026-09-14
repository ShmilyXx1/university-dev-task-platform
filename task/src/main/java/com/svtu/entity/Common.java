package com.svtu.entity;

import com.svtu.exception.AdminException;
import com.svtu.exception.UserException;
import com.svtu.mapper.OrderMapper;
import com.svtu.mapper.FeedbackMapper;
import com.svtu.mapper.UserMapper;
import com.svtu.mapper.UserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Common {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;

    /**
     * 查询用户拥有的所有角色名称（来源：t_user_role 关联 t_role）
     */
    public List<String> getRoleNames(int userId) {
        return userRoleMapper.selectUserRoleName(userId);
    }

    /**
     * 判断用户是否拥有指定角色
     * @param roleName 角色名称，如 "管理员"、"审核员"、"客服"
     */
    public boolean hasRole(int userId, String roleName) {
        List<String> roles = userRoleMapper.selectUserRoleName(userId);
        return roles != null && roles.stream().anyMatch(r -> r != null && r.contains(roleName));
    }

    /**
     * 判断用户是否为管理员
     */
    public boolean isAdmin(int userId) {
        return hasRole(userId, "管理员");
    }
    public void checkLogin(User user) {
        checkPhone(user.getPhone());
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new UserException(501, "密码不能为空");
        }
    }


    public void checkRegister(User user) {
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new UserException(501, "密码不能为空");
        }
        if(user.getUsername()==null || user.getUsername().trim().isEmpty()){
            throw new UserException(501, "用户名不能为空");
        }
    }

    public void checkPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new UserException(501, "手机号为空");
        }
        // 大陆手机号正则（最通用版本）
        String regex = "^1[3-9]\\d{9}$";
        if (!phone.matches(regex)) {
            throw new UserException(501, "手机号不合法");
        }
    }

    public void checkPassword(String newPassword,String againPassword){
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new UserException(501, "新密码不能为空");
        }

        if (againPassword == null || againPassword.trim().isEmpty()) {
            throw new UserException(501, "确认密码不能为空");
        }
        if(!newPassword.equals(againPassword)){
            throw new UserException(501,"两次密码不一致");
        }
    }
//检查userId
    public void checkUserId(int userId){
        if (userId<=0){
            throw new AdminException(502,"用户编号错误");
        }
        User user = userMapper.selectById(userId);
        if(user.getState().equals("1")){
            throw new AdminException(502,"该用户已经被冻结请联系管理员");
        }else if (user.getState().equals("2")){
            throw new AdminException(502,"该用户已经被封禁请联系管理员");
        }
        if (user==null){
            throw new AdminException(502,"没有此用户");
        }
    }

    public void checkOrderId(int orderId){
        if (orderId<=0){
            throw new AdminException(502,"订单编号错误");
        }
        Order order = orderMapper.selectById(orderId);
        if (order==null){
            throw new AdminException(502,"没有此订单");
        }
    }

    public void checkFeedbackId(int feedbackId){
        if (feedbackId<=0){
            throw new UserException(501,"问题反馈编号错误");
        }
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback==null){
            throw new UserException(501,"没有此问题反馈");
        }
    }

}
