package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.entity.MenuRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MenuRoleMapper extends BaseMapper<MenuRole> {
    List<String> selectMenRole(int roleId);
}
