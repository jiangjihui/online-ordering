package com.ordering.controller;

import com.ordering.common.Result;
import com.ordering.dto.CategoryDTO;
import com.ordering.entity.CategoryEntity;
import com.ordering.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "分类管理", description = "菜品分类的增删改查")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "获取所有分类", description = "按 sortOrder 排序返回分类列表，公开接口")
    @GetMapping
    public Result<List<CategoryDTO>> list() {
        List<CategoryEntity> entities = categoryService.list();
        List<CategoryDTO> dtos = entities.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .map(this::toDTO)
                .collect(Collectors.toList());
        return Result.success(dtos);
    }

    @Operation(summary = "创建分类", description = "需要 ADMIN 角色")
    @PostMapping
    public Result<CategoryDTO> create(@Valid @RequestBody CategoryDTO dto) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(dto.getName());
        entity.setNameEn(dto.getNameEn());
        entity.setSortOrder(dto.getSortOrder());
        entity.setIcon(dto.getIcon());
        categoryService.save(entity);
        return Result.success(toDTO(entity));
    }

    @Operation(summary = "更新分类", description = "需要 ADMIN 角色")
    @PutMapping("/{id}")
    public Result<CategoryDTO> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        CategoryEntity entity = categoryService.getById(id);
        if (entity == null) {
            return Result.error("Category not found");
        }
        entity.setName(dto.getName());
        entity.setNameEn(dto.getNameEn());
        entity.setSortOrder(dto.getSortOrder());
        entity.setIcon(dto.getIcon());
        categoryService.updateById(entity);
        return Result.success(toDTO(entity));
    }

    @Operation(summary = "删除分类", description = "需要 ADMIN 角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.success();
    }

    private CategoryDTO toDTO(CategoryEntity entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setNameEn(entity.getNameEn());
        dto.setSortOrder(entity.getSortOrder());
        dto.setIcon(entity.getIcon());
        return dto;
    }
}
