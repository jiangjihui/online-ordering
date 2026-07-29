package com.ordering.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryDTO {

    private Long id;
    @NotBlank(message = "分类名称不能为空")
    private String name;
    private String nameEn;
    private Integer sortOrder;
    private String icon;
}
