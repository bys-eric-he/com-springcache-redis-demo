package com.springcache.redis.demo.service.impl;

import com.springcache.redis.demo.entity.ProductInfo;
import com.springcache.redis.demo.service.ProductInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 产品信息
 */
@Slf4j
@Service
public class ProductInfoServiceImpl implements ProductInfoService {
    private static final Map<Long, ProductInfo> DATABASES = new HashMap<>();

    static {
        DATABASES.put(1L, new ProductInfo(1L, 3, "Iphone XS"));
        DATABASES.put(2L, new ProductInfo(2L, 2, "Iphone XS Plus"));
        DATABASES.put(3L, new ProductInfo(3L, 2, "Iphone 10"));
    }

    /**
     * 获取产品信息
     *
     * @param productId
     * @return
     */
    @Cacheable(value = "productInfoCache", key = "'product:' + #productId", condition = "#productId < 10")
    @Override
    public ProductInfo selectByPrimaryKey(long productId) {
        log.info("----进入 selectByPrimaryKey 方法----");
        return DATABASES.get(productId);
    }

    /**
     * 更新产品信息
     *
     * @param productInfo
     */
    @CachePut(value = "productInfoCache", key = "'product:' + #productInfo.productId", condition = "#productInfo.productName.length() < 10")
    @Override
    public ProductInfo updateByPrimaryKey(ProductInfo productInfo) {
        DATABASES.put(productInfo.getProductId(), productInfo);
        log.info("----进入 updateByPrimaryKey 方法----");
        return productInfo;
    }
}
