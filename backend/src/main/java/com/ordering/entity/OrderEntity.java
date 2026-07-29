package com.ordering.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;




@Data
@TableName("orders")
public class OrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("table_id")
    private Long tableId;

    @TableField("order_type")
    private String orderType = "scan";

    @TableField("status")
    private String status = "pending";

    @TableField("total_amount")
    private Double totalAmount;

    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private String createdAt;

    @TableField("started_at")
    private String startedAt;

    @TableField("completed_at")
    private String completedAt;
}
