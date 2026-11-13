package com.chuanzhi.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanzhi.health.entity.Package;
import com.chuanzhi.health.entity.PackageItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PackageMapper extends BaseMapper<Package> {

    @Select("SELECT pi.*, ei.name AS exam_item_name FROM package_items pi LEFT JOIN exam_items ei ON pi.exam_item_id = ei.id WHERE pi.package_id = #{packageId} ORDER BY pi.sort_order")
    List<PackageItemVO> selectPackageItems(Long packageId);
}
