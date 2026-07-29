package com.ordering.controller;

import com.ordering.common.Result;
import com.ordering.dto.DishDTO;
import com.ordering.entity.DishEntity;
import com.ordering.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "菜品管理", description = "菜品的增删改查、图片上传、售罄开关")
@RestController
@RequestMapping("/api/dishes")
public class DishController {

    @Autowired
    private DishService dishService;

    @Value("${app.upload.dir:${user.dir}/data/images}")
    private String uploadDir;

    @Operation(summary = "上传菜品图片", description = "需要 ADMIN 角色，上传后返回图片文件名")
    @PostMapping("/{id}/image")
    public Result<String> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) return Result.error("文件为空");
        DishEntity dish = dishService.getById(id);
        if (dish == null) return Result.error("菜品不存在");

        String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + ext;

        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) Files.createDirectories(dirPath);
        file.transferTo(dirPath.resolve(filename));

        dish.setImage(filename);
        dishService.updateById(dish);
        return Result.success(filename);
    }

    @Operation(summary = "获取菜品列表", description = "公开接口，支持按分类、状态、搜索、售罄筛选")
    @GetMapping
    public Result<List<DishDTO>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean soldOut) {

        List<DishEntity> entities;
        if (categoryId == null && status == null && (search == null || search.isEmpty()) && soldOut == null) {
            entities = dishService.list();
        } else {
            entities = dishService.findByCondition(categoryId, status, search, soldOut);
        }
        List<DishDTO> dtos = entities.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    @Operation(summary = "获取菜品详情", description = "公开接口")
    @GetMapping("/{id}")
    public Result<DishDTO> getById(@PathVariable Long id) {
        DishEntity entity = dishService.getById(id);
        if (entity == null) {
            return Result.error("Dish not found");
        }
        return Result.success(toDTO(entity));
    }

    @Operation(summary = "创建菜品", description = "需要 ADMIN 角色")
    @PostMapping
    public Result<DishDTO> create(@Valid @RequestBody DishDTO dto) {
        DishEntity entity = new DishEntity();
        entity.setName(dto.getName());
        entity.setNameEn(dto.getNameEn());
        entity.setCategoryId(dto.getCategoryId());
        entity.setPrice(dto.getPrice());
        entity.setDescription(dto.getDescription());
        entity.setDescriptionEn(dto.getDescriptionEn());
        entity.setImage(dto.getImage());
        entity.setStatus(dto.getStatus());
        entity.setSoldOut(dto.getSoldOut());
        entity.setIsSpicy(dto.getIsSpicy());
        entity.setLabels(dto.getLabels());
        entity.setSortOrder(dto.getSortOrder());
        dishService.save(entity);
        return Result.success(toDTO(entity));
    }

    @Operation(summary = "更新菜品", description = "需要 ADMIN 角色，含售罄开关、上架状态")
    @PutMapping("/{id}")
    public Result<DishDTO> update(@PathVariable Long id, @Valid @RequestBody DishDTO dto) {
        DishEntity entity = dishService.getById(id);
        if (entity == null) {
            return Result.error("Dish not found");
        }
        entity.setName(dto.getName());
        entity.setNameEn(dto.getNameEn());
        entity.setCategoryId(dto.getCategoryId());
        entity.setPrice(dto.getPrice());
        entity.setDescription(dto.getDescription());
        entity.setDescriptionEn(dto.getDescriptionEn());
        entity.setImage(dto.getImage());
        entity.setStatus(dto.getStatus());
        entity.setSoldOut(dto.getSoldOut());
        entity.setIsSpicy(dto.getIsSpicy());
        entity.setLabels(dto.getLabels());
        entity.setSortOrder(dto.getSortOrder());
        dishService.updateById(entity);
        return Result.success(toDTO(entity));
    }

    @Operation(summary = "删除菜品", description = "需要 ADMIN 角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dishService.removeById(id);
        return Result.success();
    }

    private DishDTO toDTO(DishEntity entity) {
        DishDTO dto = new DishDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setNameEn(entity.getNameEn());
        dto.setCategoryId(entity.getCategoryId());
        dto.setPrice(entity.getPrice());
        dto.setDescription(entity.getDescription());
        dto.setDescriptionEn(entity.getDescriptionEn());
        dto.setImage(entity.getImage());
        dto.setStatus(entity.getStatus());
        dto.setSoldOut(entity.getSoldOut());
        dto.setIsSpicy(entity.getIsSpicy());
        dto.setLabels(entity.getLabels());
        dto.setSortOrder(entity.getSortOrder());
        return dto;
    }
}
