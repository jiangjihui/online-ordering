package com.ordering.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ordering.entity.TableEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TableMapper extends BaseMapper<TableEntity> {
}
