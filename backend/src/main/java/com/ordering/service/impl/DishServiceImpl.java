package com.ordering.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ordering.entity.DishEntity;
import com.ordering.mapper.DishMapper;
import com.ordering.service.DishService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, DishEntity> implements DishService {

    @Override
    public List<DishEntity> findByCondition(Long categoryId, String status, String search, Boolean soldOut) {
        return baseMapper.findByCondition(categoryId, status, search, soldOut);
    }
}
