package com.ordering.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ordering.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {

    @Select("SELECT * FROM order_items WHERE order_id = #{orderId}")
    List<OrderItemEntity> findByOrderId(@Param("orderId") Long orderId);
}
