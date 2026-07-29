package com.ordering.dto;

import lombok.Data;



@Data
public class TopDishDTO {

    private Long dishId;
    private String dishName;
    private Integer totalQuantity;
    private Double totalRevenue;
}
