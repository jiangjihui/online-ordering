package com.ordering.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ordering.entity.WaiterCallEntity;
import com.ordering.mapper.WaiterCallMapper;
import com.ordering.service.WaiterCallService;
import com.ordering.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WaiterCallServiceImpl extends ServiceImpl<WaiterCallMapper, WaiterCallEntity> implements WaiterCallService {

    @Autowired
    private WebSocketService wsService;

    @Override
    public List<WaiterCallEntity> getPendingCalls() {
        return lambdaQuery().eq(WaiterCallEntity::getStatus, "pending").list();
    }

    @Override
    public void handleCall(Long id) {
        WaiterCallEntity call = getById(id);
        if (call != null) {
            call.setStatus("handled");
            updateById(call);
            wsService.notifyWaiterCallHandled(id);
        }
    }
}
