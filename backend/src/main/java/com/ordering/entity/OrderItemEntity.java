package com.ordering.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;



@Data
@TableName("order_items")
public class OrderItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("dish_id")
    private Long dishId;

    @TableField("dish_name")
    private String dishName;

    @TableField("quantity")
    private Integer quantity = 1;

    private String remark;

    @TableField("status")
    private String status = "pending";

    @TableField("started_at")
    private String startedAt;

    @TableField("completed_at")
    private String completedAt;
}
