package com.tut2.group3.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tut2.group3.warehouse.dto.response.ProductPriceResponse;
import com.tut2.group3.warehouse.dto.response.ProductWithStockDto;
import com.tut2.group3.warehouse.entity.Inventory;
import com.tut2.group3.warehouse.entity.Product;
import com.tut2.group3.warehouse.mapper.InventoryMapper;
import com.tut2.group3.warehouse.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;

    /**
     * Get product by ID
     */
    public Product getProductById(Long id) {
        log.info("Querying product by id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        return product;
    }

    /**
     * Get product by name
     */
    public Product getProductByName(String name) {
        log.info("Querying product by name: {}", name);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getName, name);
        Product product = productMapper.selectOne(queryWrapper);
        if (product == null) {
            throw new RuntimeException("Product not found with name: " + name);
        }
        return product;
    }

    /**
     * Search products by name (fuzzy search)
     */
    public List<Product> searchProductsByName(String name) {
        log.info("Searching products by name: {}", name);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Product::getName, name);
        return productMapper.selectList(queryWrapper);
    }

    /**
     * Get all products
     */
    public List<Product> getAllProducts() {
        log.info("Querying all products");
        return productMapper.selectList(null);
    }

    /**
     * Get product price by ID
     */
    public ProductPriceResponse getProductPriceById(Long id) {
        log.info("Querying product price by id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        return ProductPriceResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .build();
    }

    /**
     * Get product price by name
     */
    public ProductPriceResponse getProductPriceByName(String name) {
        log.info("Querying product price by name: {}", name);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getName, name);
        Product product = productMapper.selectOne(queryWrapper);
        if (product == null) {
            throw new RuntimeException("Product not found with name: " + name);
        }
        return ProductPriceResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .build();
    }

    /**
     * Get all products with stock information
     * Stock is calculated from inventory table (availableQuantity only)
     * This matches the checkAvailability logic to show only purchasable stock
     */
    public List<ProductWithStockDto> getAllProductsWithStock() {
        log.info("Querying all products with stock information");
        List<Product> products = productMapper.selectList(null);

        return products.stream()
                .map(product -> {
                    // Calculate available stock from all warehouses for this product
                    LambdaQueryWrapper<Inventory> inventoryQuery = new LambdaQueryWrapper<>();
                    inventoryQuery.eq(Inventory::getProductId, product.getId());
                    List<Inventory> inventories = inventoryMapper.selectList(inventoryQuery);

                    // Sum up only availableQuantity from all warehouses
                    // This matches the checkAvailability logic
                    int availableStock = inventories.stream()
                            .mapToInt(inv ->
                                inv.getAvailableQuantity() != null ? inv.getAvailableQuantity() : 0
                            )
                            .sum();

                    return ProductWithStockDto.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .stock(availableStock)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
