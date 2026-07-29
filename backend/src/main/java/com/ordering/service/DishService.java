package com.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ordering.entity.DishEntity;

import java.util.List;

public interface DishService extends IService<DishEntity> {

    List<DishEntity> findByCondition(Long categoryId, String status, String search, Boolean soldOut);
}
