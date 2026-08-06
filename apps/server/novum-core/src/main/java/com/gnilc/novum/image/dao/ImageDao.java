package com.gnilc.novum.image.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.novum.image.entity.bo.ImageBo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImageDao extends BaseMapper<ImageBo> {
    @Delete("DELETE FROM sys_image WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);
}
