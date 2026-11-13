package com.chuanzhi.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanzhi.health.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
