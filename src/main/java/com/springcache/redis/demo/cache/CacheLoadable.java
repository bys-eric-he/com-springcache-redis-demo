package com.springcache.redis.demo.cache;

/**
 * 抽像将数据加载到缓存的行为
 * @param <T>
 */
public interface CacheLoadable<T> {
    /**
     * 缓存数据加载方法
     *
     * @return
     */
    T load();
}
