package com.ordering.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ordering.entity.ComboItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ComboItemMapper extends BaseMapper<ComboItemEntity> {

    @Select("SELECT * FROM combo_items WHERE combo_id = #{comboId}")
    List<ComboItemEntity> findByComboId(@Param("comboId") Long comboId);
}
