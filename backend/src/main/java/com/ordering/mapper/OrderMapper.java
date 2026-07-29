package com.ordering.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ordering.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    @Select("<script>" +
            "SELECT * FROM orders WHERE 1=1" +
            "<if test='tableId != null'> AND table_id = #{tableId}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='startDate != null and startDate != \"\"'> AND created_at &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'> AND created_at &lt;= #{endDate}</if>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<OrderEntity> findByCondition(@Param("tableId") Long tableId,
                                      @Param("status") String status,
                                      @Param("startDate") String startDate,
                                      @Param("endDate") String endDate);
}
