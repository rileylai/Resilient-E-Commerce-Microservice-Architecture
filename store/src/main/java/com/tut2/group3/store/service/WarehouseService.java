package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.warehouse.WarehouseRequestDto;
import com.tut2.group3.store.dto.warehouse.WarehouseResponseDto;

public interface WarehouseService {
    WarehouseResponseDto checkOrderItem(WarehouseRequestDto warehouseRequestDto);
}
