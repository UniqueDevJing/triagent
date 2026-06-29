package com.chuanzhi.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanzhi.health.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Update("UPDATE notifications SET is_read = 1 WHERE user_id = #{userId}")
    int markAllRead(Long userId);
}
