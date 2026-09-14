package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.entity.Role;
import com.svtu.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    Role selectByRoleName(@Param("roleName") String roleName);

}
