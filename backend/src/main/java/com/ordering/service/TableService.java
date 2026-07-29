package com.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ordering.entity.TableEntity;

public interface TableService extends IService<TableEntity> {

    TableEntity getByNumber(String number);

    void resetTable(Long id);
}
