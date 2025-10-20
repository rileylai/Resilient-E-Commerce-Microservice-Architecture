package com.tut2.group3.warehouse.controller;

import com.tut2.group3.warehouse.common.ErrorCode;
import com.tut2.group3.warehouse.common.Result;
import com.tut2.group3.warehouse.dto.response.ProductPriceResponse;
import com.tut2.group3.warehouse.entity.Product;
import com.tut2.group3.warehouse.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Result<List<Product>> getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            String message = products.isEmpty()
                    ? "No products available"
                    : "Found " + products.size() + " product(s)";
            return Result.success(message, products);
        } catch (Exception e) {
            log.error("Error getting all products", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    @GetMapping("/price")
    public Result<ProductPriceResponse> getProductPrice(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name) {
        try {
            if (id == null && (name == null || name.trim().isEmpty())) {
                return Result.error(
                        ErrorCode.BAD_REQUEST,
                        "Either id or name parameter is required"
                );
            }

            if (id != null && name != null && !name.trim().isEmpty()) {
                return Result.error(
                        ErrorCode.BAD_REQUEST,
                        "Please provide only one parameter: either id or name"
                );
            }

            ProductPriceResponse priceResponse;
            if (id != null) {
                priceResponse = productService.getProductPriceById(id);
            } else {
                priceResponse = productService.getProductPriceByName(name.trim());
            }

            return Result.success("Product price retrieved", priceResponse);
        } catch (RuntimeException e) {
            log.error("Error querying product price", e);
            return Result.error(
                    ErrorCode.PRODUCT_NOT_FOUND,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error querying product price", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }
}
