package com.ordering.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;



@Data
@TableName("waiter_calls")
public class WaiterCallEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("table_id")
    private Long tableId;

    @TableField("table_number")
    private String tableNumber;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private String createdAt;

    @TableField("status")
    private String status = "pending";
}
