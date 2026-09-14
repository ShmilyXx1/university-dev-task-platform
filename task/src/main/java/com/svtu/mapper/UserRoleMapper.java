package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    List<String> selectUserRoleName(int userId);
}
