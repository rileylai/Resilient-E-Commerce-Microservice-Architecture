package com.tut2.group3.store.entity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Product {

    private Integer id;
    @NotEmpty
    private String productName;
    @NotNull
    private double price;
    @NotEmpty
    private int quantity;
    @NotEmpty
    private Integer storageId;

}
