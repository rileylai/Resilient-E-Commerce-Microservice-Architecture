package com.tut2.group3.store.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "warehouse-service")
public interface WarehouseClient {


}
