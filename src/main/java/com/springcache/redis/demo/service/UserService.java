package com.springcache.redis.demo.service;

import com.springcache.redis.demo.entity.User;

public interface UserService {
    /**
     * 更新
     *
     * @param user 用户对象
     * @return 操作结果
     */
    User saveOrUpdate(User user);

    /**
     * 获取
     *
     * @param id key值
     * @return 返回结果
     */
    User get(Long id);

    /**
     * 删除
     *
     * @param id key值
     */
    void delete(Long id);
}
