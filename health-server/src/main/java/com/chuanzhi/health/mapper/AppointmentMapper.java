package com.chuanzhi.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    @Select("<script>" +
            "SELECT a.*, m.name AS member_name, p.name AS package_name " +
            "FROM appointments a " +
            "LEFT JOIN members m ON a.member_id = m.id " +
            "LEFT JOIN packages p ON a.package_id = p.id " +
            "WHERE a.deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'> AND m.name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='status != null and status != \"\"'> AND a.status = #{status}</if>" +
            "ORDER BY a.created_at DESC" +
            "</script>")
    Page<Appointment> selectWithDetail(Page<?> page, @Param("keyword") String keyword, @Param("status") String status);
}
