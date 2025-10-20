package com.tut2.group3.store.client;

import com.tut2.group3.store.dto.order.OrderCreateRequestDTO;
import com.tut2.group3.store.dto.warehouse.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "warehouse", url = "http://localhost:8081")
public interface WarehouseClient {

    @GetMapping("/warehouse/products/list")
    List<ProductDto> getAllProducts();

    @PostMapping("/warehouse/checkstock")
    boolean checkStock(@RequestBody OrderCreateRequestDTO orderCreateRequestDTO);

    @PostMapping("/warehouse/getProduct")
    ProductDto getProductById(@RequestBody Long productId);

}
