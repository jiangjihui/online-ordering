package com.ordering.dto;

import lombok.Data;


import java.util.List;

@Data
public class OrderStatsDTO {

    private Integer diningTableCount;
    private Integer pendingOrderCount;
    private Integer preparingOrderCount;
    private Double todayRevenue;
    private Integer activeDishCount;
    private List<TopDishDTO> topDishes;
    private List<OrderDTO> recentCompletedOrders;
}
