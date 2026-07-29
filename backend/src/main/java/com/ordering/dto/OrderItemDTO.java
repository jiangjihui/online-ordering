package com.ordering.dto;

import lombok.Data;

@Data
public class OrderItemDTO {

    private Long id;
    private Long orderId;
    private Long dishId;
    private String dishName;
    private Integer quantity;
    private String remark;
    private String status;
    private String startedAt;
    private String completedAt;
}
