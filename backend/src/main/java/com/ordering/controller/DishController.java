package com.ordering.controller;

import com.ordering.common.Result;
import com.ordering.dto.DishDTO;
import com.ordering.entity.DishEntity;
import com.ordering.service.DishService;
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

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    @Autowired
    private DishService dishService;

    @Value("${app.upload.dir:${user.dir}/data/images}")
    private String uploadDir;

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

    @GetMapping("/{id}")
    public Result<DishDTO> getById(@PathVariable Long id) {
        DishEntity entity = dishService.getById(id);
        if (entity == null) {
            return Result.error("Dish not found");
        }
        return Result.success(toDTO(entity));
    }

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
