package com.ordering.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ordering.common.Result;
import com.ordering.dto.ComboDTO;
import com.ordering.dto.ComboItemDTO;
import com.ordering.entity.ComboEntity;
import com.ordering.entity.ComboItemEntity;
import com.ordering.mapper.ComboItemMapper;
import com.ordering.service.ComboService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/combos")
public class ComboController {

    @Autowired
    private ComboService comboService;

    @Autowired
    private ComboItemMapper comboItemMapper;

    @GetMapping
    public Result<List<ComboDTO>> list(@RequestParam(required = false) String status) {
        QueryWrapper<ComboEntity> wrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        List<ComboEntity> entities = comboService.list(wrapper);
        List<ComboDTO> dtos = entities.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    @PostMapping
    public Result<ComboDTO> create(@Valid @RequestBody ComboDTO dto) {
        ComboEntity entity = new ComboEntity();
        entity.setName(dto.getName());
        entity.setNameEn(dto.getNameEn());
        entity.setPrice(dto.getPrice());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus());
        comboService.save(entity);

        if (dto.getItems() != null) {
            for (ComboItemDTO itemDTO : dto.getItems()) {
                ComboItemEntity itemEntity = new ComboItemEntity();
                itemEntity.setComboId(entity.getId());
                itemEntity.setDishId(itemDTO.getDishId());
                itemEntity.setDishName(itemDTO.getDishName());
                itemEntity.setQuantity(itemDTO.getQuantity());
                comboItemMapper.insert(itemEntity);
            }
        }

        return Result.success(toDTO(entity));
    }

    @PutMapping("/{id}")
    public Result<ComboDTO> update(@PathVariable Long id, @Valid @RequestBody ComboDTO dto) {
        ComboEntity entity = comboService.getById(id);
        if (entity == null) {
            return Result.error("Combo not found");
        }
        entity.setName(dto.getName());
        entity.setNameEn(dto.getNameEn());
        entity.setPrice(dto.getPrice());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus());
        comboService.updateById(entity);

        // Delete old items and insert new items
        QueryWrapper<ComboItemEntity> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("combo_id", id);
        comboItemMapper.delete(deleteWrapper);

        if (dto.getItems() != null) {
            for (ComboItemDTO itemDTO : dto.getItems()) {
                ComboItemEntity itemEntity = new ComboItemEntity();
                itemEntity.setComboId(id);
                itemEntity.setDishId(itemDTO.getDishId());
                itemEntity.setDishName(itemDTO.getDishName());
                itemEntity.setQuantity(itemDTO.getQuantity());
                comboItemMapper.insert(itemEntity);
            }
        }

        return Result.success(toDTO(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // Delete combo items first
        QueryWrapper<ComboItemEntity> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("combo_id", id);
        comboItemMapper.delete(deleteWrapper);

        // Delete combo
        comboService.removeById(id);
        return Result.success();
    }

    private ComboDTO toDTO(ComboEntity entity) {
        ComboDTO dto = new ComboDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setNameEn(entity.getNameEn());
        dto.setPrice(entity.getPrice());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());

        // Fetch combo items
        List<ComboItemEntity> items = comboItemMapper.findByComboId(entity.getId());
        List<ComboItemDTO> itemDTOs = items.stream().map(this::toItemDTO).collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    private ComboItemDTO toItemDTO(ComboItemEntity entity) {
        ComboItemDTO dto = new ComboItemDTO();
        dto.setId(entity.getId());
        dto.setComboId(entity.getComboId());
        dto.setDishId(entity.getDishId());
        dto.setDishName(entity.getDishName());
        dto.setQuantity(entity.getQuantity());
        return dto;
    }
}
