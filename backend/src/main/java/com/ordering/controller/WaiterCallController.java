package com.ordering.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ordering.common.Result;
import com.ordering.dto.WaiterCallDTO;
import com.ordering.entity.WaiterCallEntity;
import com.ordering.service.WaiterCallService;
import com.ordering.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/waiter-calls")
public class WaiterCallController {

    @Autowired
    private WaiterCallService waiterCallService;

    @Autowired
    private WebSocketService wsService;

    @GetMapping
    public Result<List<WaiterCallDTO>> list(@RequestParam(required = false) String status) {
        QueryWrapper<WaiterCallEntity> wrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_at");
        List<WaiterCallEntity> entities = waiterCallService.list(wrapper);
        List<WaiterCallDTO> dtos = entities.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    @PostMapping
    public Result<WaiterCallDTO> create(@Valid @RequestBody Map<String, Object> body) {
        WaiterCallEntity entity = new WaiterCallEntity();
        entity.setTableId(Long.valueOf(body.get("tableId").toString()));
        entity.setTableNumber(body.get("tableNumber").toString());
        entity.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        entity.setStatus("pending");
        waiterCallService.save(entity);
        wsService.notifyWaiterCallCreated(toDTO(entity));
        return Result.success(toDTO(entity));
    }

    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id) {
        waiterCallService.handleCall(id);
        return Result.success();
    }

    private WaiterCallDTO toDTO(WaiterCallEntity entity) {
        WaiterCallDTO dto = new WaiterCallDTO();
        dto.setId(entity.getId());
        dto.setTableId(entity.getTableId());
        dto.setTableNumber(entity.getTableNumber());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
