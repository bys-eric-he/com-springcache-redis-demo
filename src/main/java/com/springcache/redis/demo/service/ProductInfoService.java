package com.springcache.redis.demo.service;

import com.springcache.redis.demo.entity.ProductInfo;

public interface ProductInfoService {
    /**
     * 获取产品信息
     * @param productId
     * @return
     */
    ProductInfo selectByPrimaryKey(long productId);

    /**
     * 更新产品信息
     * @param productInfo
     */
    ProductInfo updateByPrimaryKey(ProductInfo productInfo);
}
