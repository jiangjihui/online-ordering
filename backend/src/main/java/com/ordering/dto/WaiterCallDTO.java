package com.ordering.dto;

import lombok.Data;

@Data
public class WaiterCallDTO {

    private Long id;
    private Long tableId;
    private String tableNumber;
    private String createdAt;
    private String status;
}
