package com.ordering.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("combo_items")
public class ComboItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("combo_id")
    private Long comboId;

    @TableField("dish_id")
    private Long dishId;

    @TableField("dish_name")
    private String dishName;

    @TableField("quantity")
    private Integer quantity = 1;
}
