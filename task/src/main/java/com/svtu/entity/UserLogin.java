package com.svtu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 添加这个注解，ignoreUnknown = true 表示忽略未知字段
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLogin implements UserDetails {
    private User user;

    //存储权限信息
    private List<String> permissions;

    @JsonIgnore
    private List<GrantedAuthority> authorities;

    public UserLogin(User user,List<String> permissions){
        this.user=user;
        this.permissions=permissions;
    }
    public UserLogin(User user){
        this.user=user;
    }
    @Override
    //返回当前登录用户所拥有的权限 / 角色集合
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(authorities != null){
            return authorities;
        }
//        //第一种用增强for
////        authorities=new ArrayList<>();
////        for(String permission:permissions){
////            SimpleGrantedAuthority authority=new SimpleGrantedAuthority(permission);
////            authorities.add(authority);
////        }
//
        //第二种方式
        //把permission中的字符串类型的权限信息转换成GrantedAuthority对象存入authorities中
        if (permissions==null){
            return Collections.emptyList();
        }
        authorities=permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
