package com.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ordering.dto.OrderCreateRequest;
import com.ordering.dto.OrderStatsDTO;
import com.ordering.entity.OrderEntity;
import com.ordering.entity.OrderItemEntity;

import java.util.List;

public interface OrderService extends IService<OrderEntity> {

    OrderEntity createOrder(OrderCreateRequest request);

    List<OrderEntity> findByCondition(Long tableId, String status, String startDate, String endDate);

    OrderItemEntity updateItemStatus(Long orderId, Long itemId, String status);

    void completeAllItems(Long orderId);

    OrderStatsDTO getStats();
}
