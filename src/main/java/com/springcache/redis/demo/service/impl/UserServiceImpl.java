package com.springcache.redis.demo.service.impl;

import com.springcache.redis.demo.cache.CacheLoadable;
import com.springcache.redis.demo.cache.CacheTemplate;
import com.springcache.redis.demo.entity.User;
import com.springcache.redis.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final Map<Long, User> DATABASES = new HashMap<>();

    @Autowired
    private CacheTemplate<User> cacheTemplate;

    static {
        DATABASES.put(10001L, new User(10001L, "he yong", "heyong@1988"));
        DATABASES.put(10002L, new User(10002L, "eric.he", "eric@1988"));
        DATABASES.put(10003L, new User(10003L, "sky.zhang", "sky@1987"));
        DATABASES.put(10004L, new User(10004L, "alex.zheng", "alex@1989"));
        DATABASES.put(10005L, new User(10005L, "felix.huang", "felix@1987"));
        DATABASES.put(10006L, new User(10006L, "robert.luo", "luobo@1987"));
    }

    /**
     * 更新
     *
     * @param user 用户对象
     * @return 操作结果
     */
    @CachePut(value = "userCache", key = "'user:' + #user.id", condition = "#user.username.length() < 10")
    @Override
    public User saveOrUpdate(User user) {
        DATABASES.put(user.getId(), user);
        log.info("----进入 saveOrUpdate 方法----");
        return user;
    }

    /**
     * 获取
     *
     * @param id key值
     * @return 返回结果
     */
    @Override
    public User get(Long id) {
        return cacheTemplate.getCacheData("cacheTemplate::userCache::user:" + id,
                10000, TimeUnit.MINUTES, new CacheLoadable<User>() {
                    @Override
                    public User load() {
                        log.info("----进入 get 方法----");
                        return DATABASES.get(id);
                    }
                }, User.class);
    }

    /**
     * 删除
     *
     * @param id key值
     */
    @CacheEvict(value = "userCache", key = "'user:' + #id")
    @Override
    public void delete(Long id) {
        DATABASES.remove(id);
        log.info("----进入 delete 方法----");
    }
}
