package com.ordering.controller;

import com.ordering.common.Result;
import com.ordering.dto.TableDTO;
import com.ordering.entity.TableEntity;
import com.ordering.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    @Autowired
    private TableService tableService;

    @GetMapping
    public Result<List<TableDTO>> list() {
        List<TableEntity> entities = tableService.list();
        List<TableDTO> dtos = entities.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    @GetMapping("/{id}")
    public Result<TableDTO> getById(@PathVariable Long id) {
        TableEntity entity = tableService.getById(id);
        if (entity == null) {
            return Result.error("Table not found");
        }
        return Result.success(toDTO(entity));
    }

    @GetMapping("/number/{number}")
    public Result<TableDTO> getByNumber(@PathVariable String number) {
        TableEntity entity = tableService.getByNumber(number);
        if (entity == null) {
            return Result.error("Table not found");
        }
        return Result.success(toDTO(entity));
    }

    @PostMapping
    public Result<TableDTO> create(@Valid @RequestBody TableDTO dto) {
        TableEntity entity = new TableEntity();
        entity.setNumber(dto.getNumber());
        entity.setArea(dto.getArea());
        entity.setCapacity(dto.getCapacity());
        entity.setStatus(dto.getStatus());
        tableService.save(entity);
        return Result.success(toDTO(entity));
    }

    @PutMapping("/{id}")
    public Result<TableDTO> update(@PathVariable Long id, @Valid @RequestBody TableDTO dto) {
        TableEntity entity = tableService.getById(id);
        if (entity == null) {
            return Result.error("Table not found");
        }
        entity.setNumber(dto.getNumber());
        entity.setArea(dto.getArea());
        entity.setCapacity(dto.getCapacity());
        entity.setStatus(dto.getStatus());
        tableService.updateById(entity);
        return Result.success(toDTO(entity));
    }

    @PutMapping("/{id}/reset")
    public Result<Void> reset(@PathVariable Long id) {
        tableService.resetTable(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tableService.removeById(id);
        return Result.success();
    }

    private TableDTO toDTO(TableEntity entity) {
        TableDTO dto = new TableDTO();
        dto.setId(entity.getId());
        dto.setNumber(entity.getNumber());
        dto.setArea(entity.getArea());
        dto.setCapacity(entity.getCapacity());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
