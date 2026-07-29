package com.ordering.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ordering.entity.OrderEntity;
import com.ordering.entity.TableEntity;
import com.ordering.mapper.OrderMapper;
import com.ordering.mapper.TableMapper;
import com.ordering.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TableServiceImpl extends ServiceImpl<TableMapper, TableEntity> implements TableService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public TableEntity getByNumber(String number) {
        return lambdaQuery().eq(TableEntity::getNumber, number).one();
    }

    @Override
    @Transactional
    public void resetTable(Long id) {
        TableEntity table = getById(id);
        if (table != null && "dining".equals(table.getStatus())) {
            table.setStatus("idle");
            updateById(table);

            // Close all orders for this table
            List<OrderEntity> orders = orderMapper.findByCondition(table.getId(), null, null, null);
            for (OrderEntity order : orders) {
                if (!"closed".equals(order.getStatus())) {
                    order.setStatus("closed");
                    orderMapper.updateById(order);
                }
            }
        }
    }
}
