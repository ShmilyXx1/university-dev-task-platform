package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.VO.UserVO;
import com.svtu.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    UserVO selectByUsernameUser(String username);
    Integer updateUser(@Param("user") User user);
    Integer updatePhone(@Param("phone") String phone,@Param("userId") int userId);
    UserVO selectByUserId(int userId);
    User selectUserId(String username);

}
