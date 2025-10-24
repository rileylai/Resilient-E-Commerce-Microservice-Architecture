package com.tut2.group3.store.controller;

import com.tut2.group3.store.client.WarehouseClient;
import com.tut2.group3.store.dto.warehouse.ProductDto;
import com.tut2.group3.store.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product Controller
 * Provides endpoints to retrieve product information from the warehouse
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private WarehouseClient warehouseClient;

    /**
     * Get all products from warehouse
     * 
     * @return Result with list of all products
     */
    @GetMapping("/all")
    public Result<List<ProductDto>> getAllProducts() {
        log.info("Received request to get all products");
        try {
            Result<List<ProductDto>> result = warehouseClient.getAllProducts();
            log.info("Successfully retrieved {} products", 
                    result.getData() != null ? result.getData().size() : 0);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving products: {}", e.getMessage(), e);
            return Result.error(500, "Failed to retrieve products: " + e.getMessage());
        }
    }
}
