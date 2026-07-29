package com.ordering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class DishDTO {

    private Long id;
    @NotBlank(message = "菜品名称不能为空")
    private String name;
    private String nameEn;
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
    private Double price;
    private String description;
    private String descriptionEn;
    private String image;
    private String status;
    private Boolean soldOut;
    private Integer isSpicy;
    private List<String> labels;
    private Integer sortOrder;
}
