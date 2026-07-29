package com.ordering.controller;

import com.ordering.common.Result;
import com.ordering.dto.OrderCreateRequest;
import com.ordering.dto.OrderDTO;
import com.ordering.dto.OrderItemDTO;
import com.ordering.dto.OrderStatsDTO;
import com.ordering.entity.OrderEntity;
import com.ordering.entity.OrderItemEntity;
import com.ordering.mapper.OrderItemMapper;
import com.ordering.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "订单管理", description = "订单创建、查询、状态更新、仪表盘统计")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Operation(summary = "获取订单列表", description = "公开接口，支持按桌号、状态、日期范围筛选")
    @GetMapping
    public Result<List<OrderDTO>> list(
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<OrderEntity> entities;
        if (tableId == null && status == null && (startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty())) {
            entities = orderService.list();
        } else {
            entities = orderService.findByCondition(tableId, status, startDate, endDate);
        }
        List<OrderDTO> dtos = entities.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    @Operation(summary = "获取订单详情", description = "公开接口，含订单项明细")
    @GetMapping("/{id}")
    public Result<OrderDTO> getById(@PathVariable Long id) {
        OrderEntity entity = orderService.getById(id);
        if (entity == null) {
            return Result.error("Order not found");
        }
        return Result.success(toDTO(entity));
    }

    @Operation(summary = "创建订单", description = "公开接口，顾客下单后自动将餐桌状态变为用餐中")
    @PostMapping
    public Result<OrderDTO> create(@Valid @RequestBody OrderCreateRequest request) {
        OrderEntity entity = orderService.createOrder(request);
        return Result.success(toDTO(entity));
    }

    @Operation(summary = "更新单品制作状态", description = "需要 ADMIN/KITCHEN 角色，状态流转: pending → preparing → completed，自动级联订单状态")
    @PutMapping("/{id}/items/{itemId}/status")
    public Result<OrderItemDTO> updateItemStatus(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody Map<String, String> body) {
        String status = body.get("status");
        OrderItemEntity itemEntity = orderService.updateItemStatus(id, itemId, status);
        if (itemEntity == null) {
            return Result.error("Order item not found");
        }
        return Result.success(toItemDTO(itemEntity));
    }

    @Operation(summary = "整单全部完成", description = "需要 ADMIN/KITCHEN 角色，将所有未完成菜品一次性标记为已完成")
    @PutMapping("/{id}/complete-all")
    public Result<Void> completeAllItems(@PathVariable Long id) {
        orderService.completeAllItems(id);
        return Result.success();
    }

    @Operation(summary = "获取仪表盘统计数据", description = "需要 ADMIN/KITCHEN 角色，含营收、订单数、热门菜品TOP5")
    @GetMapping("/stats")
    public Result<OrderStatsDTO> getStats() {
        OrderStatsDTO stats = orderService.getStats();
        return Result.success(stats);
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
        List<OrderItemDTO> itemDTOs = items.stream().map(this::toItemDTO).collect(Collectors.toList());
        dto.setItems(itemDTOs);

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
