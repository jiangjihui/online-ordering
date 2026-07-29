package com.ordering.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ordering.dto.OrderCreateRequest;
import com.ordering.dto.OrderDTO;
import com.ordering.dto.OrderItemCreateRequest;
import com.ordering.dto.OrderItemDTO;
import com.ordering.dto.OrderStatsDTO;
import com.ordering.dto.TopDishDTO;
import com.ordering.entity.DishEntity;
import com.ordering.entity.OrderEntity;
import com.ordering.entity.OrderItemEntity;
import com.ordering.entity.TableEntity;
import com.ordering.mapper.DishMapper;
import com.ordering.mapper.OrderItemMapper;
import com.ordering.mapper.OrderMapper;
import com.ordering.mapper.TableMapper;
import com.ordering.service.OrderService;
import com.ordering.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private TableMapper tableMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private WebSocketService wsService;

    private String nowStr() {
        return LocalDateTime.now().format(DTF);
    }

    @Override
    @Transactional
    public OrderEntity createOrder(OrderCreateRequest request) {
        OrderEntity order = new OrderEntity();
        order.setTableId(request.getTableId());
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "scan");
        order.setRemark(request.getRemark());
        order.setStatus("pending");
        order.setTotalAmount(request.getTotalAmount() != null ? request.getTotalAmount() : 0.0);
        order.setCreatedAt(nowStr());

        save(order);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (OrderItemCreateRequest itemReq : request.getItems()) {
                OrderItemEntity item = new OrderItemEntity();
                item.setOrderId(order.getId());
                item.setDishId(itemReq.getDishId());
                item.setDishName(itemReq.getDishName());
                item.setQuantity(itemReq.getQuantity());
                item.setRemark(itemReq.getRemark());
                item.setStatus("pending");
                orderItemMapper.insert(item);
            }
        }

        TableEntity table = tableMapper.selectById(request.getTableId());
        if (table != null && ("idle".equals(table.getStatus()) || "reserved".equals(table.getStatus()))) {
            table.setStatus("dining");
            tableMapper.updateById(table);
        }

        wsService.notifyOrderCreated(toDTO(order));

        return order;
    }

    @Override
    public List<OrderEntity> findByCondition(Long tableId, String status, String startDate, String endDate) {
        return baseMapper.findByCondition(tableId, status, startDate, endDate);
    }

    @Override
    @Transactional
    public OrderItemEntity updateItemStatus(Long orderId, Long itemId, String status) {
        OrderItemEntity item = orderItemMapper.selectById(itemId);
        if (item == null || !item.getOrderId().equals(orderId)) {
            throw new RuntimeException("Order item not found or does not belong to the order");
        }

        if ("preparing".equals(status) && "pending".equals(item.getStatus())) {
            item.setStartedAt(nowStr());
        }
        if ("completed".equals(status)) {
            item.setCompletedAt(nowStr());
        }
        item.setStatus(status);
        orderItemMapper.updateById(item);

        OrderEntity order = getById(orderId);
        List<OrderItemEntity> allItems = orderItemMapper.findByOrderId(orderId);

        if ("preparing".equals(status) && "pending".equals(order.getStatus())) {
            order.setStatus("preparing");
            order.setStartedAt(nowStr());
            updateById(order);
        }

        boolean allCompleted = allItems.stream().allMatch(i -> "completed".equals(i.getStatus()));
        if (allCompleted) {
            order.setStatus("completed");
            order.setCompletedAt(nowStr());
            updateById(order);
            wsService.notifyOrderCompleted(orderId, order.getTableId());
        } else {
            wsService.notifyItemStatusUpdated(orderId, order.getTableId(), toItemDTO(item));
        }

        return item;
    }

    @Override
    @Transactional
    public void completeAllItems(Long orderId) {
        List<OrderItemEntity> items = orderItemMapper.findByOrderId(orderId);
        String now = nowStr();

        for (OrderItemEntity item : items) {
            if ("pending".equals(item.getStatus())) {
                item.setStartedAt(now);
            }
            item.setStatus("completed");
            item.setCompletedAt(now);
            orderItemMapper.updateById(item);
        }

        OrderEntity order = getById(orderId);
        if (order.getStartedAt() == null) {
            order.setStartedAt(now);
        }
        order.setStatus("completed");
        order.setCompletedAt(now);
        updateById(order);
        wsService.notifyOrderCompleted(orderId, order.getTableId());
    }

    @Override
    public OrderStatsDTO getStats() {
        OrderStatsDTO stats = new OrderStatsDTO();

        Long diningTableCount = tableMapper.selectCount(
                new LambdaQueryWrapper<TableEntity>().eq(TableEntity::getStatus, "dining"));
        stats.setDiningTableCount(diningTableCount.intValue());

        Long pendingOrderCount = count(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getStatus, "pending"));
        stats.setPendingOrderCount(pendingOrderCount.intValue());

        Long preparingOrderCount = count(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getStatus, "preparing"));
        stats.setPreparingOrderCount(preparingOrderCount.intValue());

        String today = LocalDateTime.now().format(DAY_FMT);
        String tomorrow = LocalDateTime.now().plusDays(1).format(DAY_FMT);
        List<OrderEntity> completedToday = list(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getStatus, "completed")
                        .ge(OrderEntity::getCreatedAt, today)
                        .lt(OrderEntity::getCreatedAt, tomorrow));
        Double todayRevenue = completedToday.stream()
                .map(OrderEntity::getTotalAmount)
                .filter(a -> a != null)
                .reduce(0.0, (a, b) -> a + b);
        stats.setTodayRevenue(todayRevenue);

        Long activeDishCount = dishMapper.selectCount(
                new LambdaQueryWrapper<DishEntity>().eq(DishEntity::getStatus, "active"));
        stats.setActiveDishCount(activeDishCount.intValue());

        List<OrderItemEntity> allItems = orderItemMapper.selectList(null);
        java.util.Map<Long, TopDishDTO> dishMap = new java.util.LinkedHashMap<>();
        for (OrderItemEntity item : allItems) {
            TopDishDTO existing = dishMap.get(item.getDishId());
            if (existing == null) {
                existing = new TopDishDTO();
                existing.setDishId(item.getDishId());
                existing.setDishName(item.getDishName());
                existing.setTotalQuantity(item.getQuantity());
                existing.setTotalRevenue(0.0);
                dishMap.put(item.getDishId(), existing);
            } else {
                existing.setTotalQuantity(existing.getTotalQuantity() + item.getQuantity());
            }
        }
        // Fill revenue from dish prices
        for (java.util.Map.Entry<Long, TopDishDTO> entry : dishMap.entrySet()) {
            DishEntity dish = dishMapper.selectById(entry.getKey());
            if (dish != null) {
                entry.getValue().setTotalRevenue(dish.getPrice() * entry.getValue().getTotalQuantity());
            }
        }
        List<TopDishDTO> topDishes = dishMap.values().stream()
                .sorted((a, b) -> b.getTotalQuantity() - a.getTotalQuantity())
                .limit(5)
                .collect(Collectors.toList());
        stats.setTopDishes(topDishes);

        List<OrderEntity> recentOrders = list(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getStatus, "completed")
                        .isNotNull(OrderEntity::getCompletedAt)
                        .orderByDesc(OrderEntity::getCompletedAt)
                        .last("LIMIT 5"));
        List<OrderDTO> recentOrderDTOs = recentOrders.stream().map(order -> {
            OrderDTO dto = new OrderDTO();
            dto.setId(order.getId());
            dto.setTableId(order.getTableId());
            dto.setOrderType(order.getOrderType());
            dto.setStatus(order.getStatus());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setRemark(order.getRemark());
            dto.setCreatedAt(order.getCreatedAt());
            dto.setStartedAt(order.getStartedAt());
            dto.setCompletedAt(order.getCompletedAt());
            return dto;
        }).collect(Collectors.toList());
        stats.setRecentCompletedOrders(recentOrderDTOs);

        return stats;
    }

    private OrderDTO toDTO(OrderEntity entity) {
        OrderDTO dto = new OrderDTO();
        dto.setId(entity.getId());
        dto.setTableId(entity.getTableId());
        dto.setOrderType(entity.getOrderType());
        dto.setStatus(entity.getStatus());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setRemark(entity.getRemark());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        List<OrderItemEntity> items = orderItemMapper.findByOrderId(entity.getId());
        dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
        return dto;
    }

    private OrderItemDTO toItemDTO(OrderItemEntity entity) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setDishId(entity.getDishId());
        dto.setDishName(entity.getDishName());
        dto.setQuantity(entity.getQuantity());
        dto.setRemark(entity.getRemark());
        dto.setStatus(entity.getStatus());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        return dto;
    }
}
