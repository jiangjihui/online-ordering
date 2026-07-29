package com.ordering.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ordering.config.JsonTypeHandler;
import lombok.Data;


import java.util.List;

@Data
@TableName(value = "dishes", autoResultMap = true)
public class DishEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("name_en")
    private String nameEn;

    @TableField("category_id")
    private Long categoryId;

    private Double price;

    private String description;

    @TableField("description_en")
    private String descriptionEn;

    private String image;

    @TableField("status")
    private String status = "active";

    @TableField("sold_out")
    private Boolean soldOut = false;

    @TableField("is_spicy")
    private Integer isSpicy = 0;

    @TableField(value = "labels", typeHandler = JsonTypeHandler.class)
    private List<String> labels;

    @TableField("sort_order")
    private Integer sortOrder;
}
