package com.ordering.dto;

import lombok.Data;

@Data
public class ComboItemDTO {

    private Long id;
    private Long comboId;
    private Long dishId;
    private String dishName;
    private Integer quantity;
}
