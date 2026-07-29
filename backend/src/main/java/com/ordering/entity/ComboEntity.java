package com.ordering.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;



@Data
@TableName("combos")
public class ComboEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("name_en")
    private String nameEn;

    private Double price;

    private String description;

    @TableField("status")
    private String status = "active";
}
