package com.ordering.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ordering.entity.ComboEntity;
import com.ordering.mapper.ComboMapper;
import com.ordering.service.ComboService;
import org.springframework.stereotype.Service;

@Service
public class ComboServiceImpl extends ServiceImpl<ComboMapper, ComboEntity> implements ComboService {
}
