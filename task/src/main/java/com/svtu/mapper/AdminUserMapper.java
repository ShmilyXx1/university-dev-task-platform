package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.entity.Role;
import com.svtu.entity.User;
import com.svtu.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapper<User> {
    List<User> selectAllUser();
    Integer updateUserState(@Param("userId") int userId,@Param("state") String state);

}
