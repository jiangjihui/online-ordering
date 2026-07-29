package com.ordering.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("categories")
public class CategoryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("name_en")
    private String nameEn;

    @TableField("sort_order")
    private Integer sortOrder = 0;

    private String icon;
}
