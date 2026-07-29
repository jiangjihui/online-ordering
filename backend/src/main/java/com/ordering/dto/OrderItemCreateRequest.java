package com.ordering.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemCreateRequest {

    @NotNull(message = "dishId不能为空")
    private Long dishId;
    @NotBlank(message = "dishName不能为空")
    private String dishName;
    @NotNull(message = "quantity不能为空")
    @Min(value = 1, message = "quantity至少为1")
    private Integer quantity;
    private String remark;
}
