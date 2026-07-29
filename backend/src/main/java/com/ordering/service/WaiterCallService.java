package com.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ordering.entity.WaiterCallEntity;

import java.util.List;

public interface WaiterCallService extends IService<WaiterCallEntity> {

    List<WaiterCallEntity> getPendingCalls();

    void handleCall(Long id);
}
