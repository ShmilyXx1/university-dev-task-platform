package com.svtu.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserForgetPasswordMapper extends BaseMapper<User> {
}
