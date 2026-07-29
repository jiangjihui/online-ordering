package com.ordering.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ordering.entity.DishEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<DishEntity> {

    @Select("<script>" +
            "SELECT * FROM dishes WHERE 1=1" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='categoryId != null'> AND category_id = #{categoryId}</if>" +
            "<if test='soldOut != null'> AND sold_out = #{soldOut}</if>" +
            "<if test='search != null and search != \"\"'> AND (name LIKE CONCAT('%', #{search}, '%') OR name_en LIKE CONCAT('%', #{search}, '%'))</if>" +
            " ORDER BY sort_order" +
            "</script>")
    List<DishEntity> findByCondition(@Param("categoryId") Long categoryId,
                                     @Param("status") String status,
                                     @Param("search") String search,
                                     @Param("soldOut") Boolean soldOut);
}
