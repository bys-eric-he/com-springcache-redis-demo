package com.springcache.redis.demo.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductInfo implements Serializable {
    private static final long serialVersionUID = 8655851615465363473L;
    private long productId;
    private int productStock;
    private String productName;

    public ProductInfo() {
    }

    public ProductInfo(long productId, int productStock, String productName) {
        this.productId = productId;
        this.productStock = productStock;
        this.productName = productName;
    }
}
