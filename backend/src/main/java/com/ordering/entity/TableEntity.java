package com.ordering.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tables")
public class TableEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String number;

    private String area;

    @TableField("capacity")
    private Integer capacity = 4;

    @TableField("status")
    private String status = "idle";
}
