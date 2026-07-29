package com.ordering.dto;

import lombok.Data;


import java.util.List;

@Data
public class OrderDTO {

    private Long id;
    private Long tableId;
    private String orderType;
    private String status;
    private Double totalAmount;
    private String remark;
    private String createdAt;
    private String startedAt;
    private String completedAt;
    private List<OrderItemDTO> items;
}
