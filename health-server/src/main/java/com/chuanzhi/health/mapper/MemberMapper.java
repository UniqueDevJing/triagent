package com.chuanzhi.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanzhi.health.entity.Member;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
