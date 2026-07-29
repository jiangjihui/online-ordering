package com.ordering.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {

    @NotNull(message = "tableId不能为空")
    private Long tableId;
    private String orderType = "scan";
    @NotNull(message = "totalAmount不能为空")
    @Positive(message = "totalAmount必须大于0")
    private Double totalAmount;
    private String remark;
    private List<OrderItemCreateRequest> items;
}
