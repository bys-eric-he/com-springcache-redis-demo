package com.springcache.redis.demo.lock;

import com.springcache.redis.demo.entity.ProductInfo;
import com.springcache.redis.demo.entity.User;
import com.springcache.redis.demo.service.ProductInfoService;
import com.springcache.redis.demo.service.UserService;
import com.springcache.redis.demo.utils.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ReentrantLock是Lock的常用实现类，lock.lock()是以lock对象为锁，代码中使用的同一个lock对象，所以使用的是同一把锁
 * 一个线程通过lock()方法拿到了锁，执行了System.out.println("执行了方法");打印出结果，
 * 之后没有调用lock.unlock()方法，所以没有释放锁，第二个线程在调用lock.lock()控制台就一直阻塞在那，说明lock.lock()是一个阻塞方法。
 * 而lock.tryLock()是不同，他是一个有返回值的方法，在没有设置longTime和TimeUnit时，即试图获取锁，
 * 没有获取到就返回false,获取到就返回true,这个方法是立即返回的，当你调用lock.lock(long time,TimeUnit unit)，
 * 意思就是 在指定时间范围内阻塞，规定时间范围内获取到锁就返回true,否则就返回false，注意：如果在当前线程中以将中断表示位设为true，
 * 例如Thread.currentThread().interrupt();执行lock.lock(long time,TimeUnit unit)就会报java.lang.InterruptedException异常
 */
@Slf4j
@Component
public class BusinessTask {
    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private JedisDistributedLock jedisDistributedLock;

    @Autowired
    private JedisUtil jedisUtil;

    /**
     * 单例模式使用会抛redis.clients.jedis.exceptions.JedisConnectionException
     * 和 redis.clients.jedis.exceptions.JedisDataException:
     */
    @Autowired
    private Jedis jedis;

    @Autowired
    private ProductInfoService productInfoService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UserService userService;

    @Autowired
    private BusinessTask businessTask;

    /**
     * 参与秒杀的用户
     */
    private Long[] userIds = {10001L, 10002L, 10003L, 10004L, 10005L, 10006L};

    /**
     * 记录秒杀成功的用户，实际情况可能存在redis
     */
    private static ConcurrentHashMap<Long, Long> killUserIdMaps = new ConcurrentHashMap<>();

    @Scheduled(cron = "0/5 * * * * ? ")
    public void doSomething() {
        for (int i = 0; i < 20; i++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    businessTask.decrementProductStoreJedis(1L, 1);
                }
            };
            thread.start();
        }

    }

    /**
     * 秒杀抢商品
     * 使用tryLock()方法只有在成功获取了锁的情况下才会返回true，如果别的线程当前正持有锁，则会立即返回false。
     * 如果为这个方法加上timeout参数，则会在等待timeout的时间才会返回false或者在获取到锁的时候返回true。
     *
     * @param productId
     * @param productQuantity
     * @return
     */
    public void decrementProductStoreTryLock(Long productId, Integer productQuantity) {
        final String lockKey = "dec_store_" + productId + "_redis_lock";
        // 随机获取访问的用户，模拟不同用户请求
        int index = (int) (Math.random() * userIds.length);
        User user = userService.get(userIds[index]);
        // 加锁
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 第一个参数为 30， 尝试获取锁的的最大等待时间为30s
            // 第二个参数为 60，  上锁成功后60s后锁自动失效
            // 尝试获取锁（非阻塞,可重入锁,不会造成死锁）
            // 当获取锁时，只有当该锁资源没有被其他线程持有才可以获取到，并且返回true，同时设置持有count为1；
            // 当获取锁时，当前线程已持有该锁，那么锁可用时，返回true，同时设置持有count加1；
            // 当获取锁时，如果其他线程持有该锁，无可用锁资源，直接返回false，这时候线程不用阻塞等待，可以先去做其他事情；
            // 即使该锁是公平锁fairLock，使用tryLock()的方式获取锁也会是非公平的方式，只要获取锁时该锁可用那么就会直接获取并返回true。这种直接插入的特性在一些特定场景是很有用的。但是如果就是想使用公平的方式的话，可以试一试tryLock(0, TimeUnit.SECONDS)，几乎跟公平锁没区别，只是会监测中断事件。
            boolean lockFlag = lock.tryLock(30, 60, TimeUnit.SECONDS);
            if (lockFlag) {
                //商品秒杀处理
                ProductInfo productInfo = productInfoService.selectByPrimaryKey(productId);

                // 做幂等性处理
                if (MapUtils.isNotEmpty(killUserIdMaps)
                        && killUserIdMaps.get(user.getId()) != null
                        && killUserIdMaps.get(user.getId()).equals(productId)) {
                    log.warn("用户：" + user.getUsername() + "---已抢到商品：" + productInfo.getProductName() + "，不可以重新领取!");
                    return;
                }

                //判断缓存的商品库存，是否被抢完了
                if (productInfo.getProductStock() > 0 || productInfo.getProductStock() >= productQuantity) {
                    log.info("剩余库存->" + productInfo.getProductStock());
                    // 将秒杀抢到商品的用户存入缓存
                    killUserIdMaps.put(user.getId(), productId);
                    // 减库存操作
                    productInfo.setProductStock(productInfo.getProductStock() - productQuantity);
                    productInfoService.updateByPrimaryKey(productInfo);
                    log.info("用户：" + user.getUsername() + "--->抢到商品：" + productInfo.getProductName());
                } else {
                    log.warn("用户：" + user.getUsername() + "--->未抢到商品<---" + productInfo.getProductName());
                }
            } else {
                log.warn("当前锁资源被占用--->未获取到锁");
            }
        } catch (Exception e) {
            log.error("秒杀出现了错误！->" + e.getMessage());
        } finally {
            // 解锁
            lock.unlock();
        }
    }

    /**
     * 减库存操作
     * Redisson实现了Lock接口，对比Lock而言，是可重入锁，功能强大，源码复杂。
     * 使用lock()和使用Synchronized关键字是一样的效果，直接去获取锁。成功了就ok了，失败了就阻塞等待了。
     * 不同的是lock锁是可重入锁
     *
     * @param productId
     * @param productQuantity
     */
    public void decrementProductStoreLock(Long productId, Integer productQuantity) {
        String key = "dec_store_" + productId + "_redis_lock";
        // 随机获取访问的用户，模拟不同用户请求
        int index = (int) (Math.random() * userIds.length);
        User user = userService.get(userIds[index]);
        RLock lock = redissonClient.getLock(key);

        try {
            // 加锁 操作很类似Java的ReentrantLock机制
            // 第一个参数 60，表示上锁成功后60s后锁自动失效
            // 尝试获取锁（阻塞,可重入锁,不会造成死锁）
            // 当锁可用，并且当前线程没有持有该锁，直接获取锁并把count set为1.
            // 当锁可用，并且当前线程已经持有该锁，直接获取锁并把count增加1.
            // 当锁不可用，那么当前线程被阻塞，休眠一直到该锁可以获取，然后把持有count设置为1.
            lock.lock(60, TimeUnit.SECONDS);
            ProductInfo productInfo = productInfoService.selectByPrimaryKey(productId);
            // 做幂等性处理
            if (MapUtils.isNotEmpty(killUserIdMaps)
                    && killUserIdMaps.get(user.getId()) != null
                    && killUserIdMaps.get(user.getId()).equals(productId)) {
                log.warn("用户：" + user.getUsername() + "---已抢到商品：" + productInfo.getProductName() + "，不可以重新领取!");
                return;
            }

            //判断缓存的商品库存，是否被抢完了
            if (productInfo.getProductStock() > 0 || productInfo.getProductStock() >= productQuantity) {
                log.info("剩余库存->" + productInfo.getProductStock());
                // 将秒杀抢到商品的用户存入缓存
                killUserIdMaps.put(user.getId(), productId);
                // 减库存操作
                productInfo.setProductStock(productInfo.getProductStock() - productQuantity);
                productInfoService.updateByPrimaryKey(productInfo);
                log.info("用户：" + user.getUsername() + "--->抢到商品：" + productInfo.getProductName());
            } else {
                log.warn("用户：" + user.getUsername() + "--->未抢到商品<---" + productInfo.getProductName());
            }
        } catch (Exception ex) {
            log.error("减库存操作异常->" + ex.getMessage());
            ex.printStackTrace();
        } finally {
            //解锁
            lock.unlock();
        }
    }

    /**
     * 减库存操作
     *
     * @param productId
     * @param productQuantity
     */
    public void decrementProductStore(Long productId, Integer productQuantity) {
        String key = "dec_store_" + productId + "_redis_lock";
        // 随机获取访问的用户，模拟不同用户请求
        int index = (int) (Math.random() * userIds.length);
        User user = userService.get(userIds[index]);

        long time = System.currentTimeMillis();
        try {
            //如果加锁失败
            if (!distributedLock.tryLock(key, String.valueOf(time), 10 * 1000)) {
                log.warn("->获取锁失败!");
                return;
            }
            ProductInfo productInfo = productInfoService.selectByPrimaryKey(productId);
            // 做幂等性处理
            if (MapUtils.isNotEmpty(killUserIdMaps)
                    && killUserIdMaps.get(user.getId()) != null
                    && killUserIdMaps.get(user.getId()).equals(productId)) {
                log.warn("用户：" + user.getUsername() + "---已抢到商品：" + productInfo.getProductName() + "，不可以重新领取!");
                return;
            }
            //判断缓存的商品库存，是否被抢完了
            if (productInfo.getProductStock() > 0 || productInfo.getProductStock() >= productQuantity) {
                log.info("剩余库存->" + productInfo.getProductStock());
                // 将秒杀抢到商品的用户存入缓存
                killUserIdMaps.put(user.getId(), productId);
                // 减库存操作
                productInfo.setProductStock(productInfo.getProductStock() - productQuantity);
                productInfoService.updateByPrimaryKey(productInfo);
                log.info("用户：" + user.getUsername() + "--->抢到商品：" + productInfo.getProductName());
            } else {
                log.warn("用户：" + user.getUsername() + "--->未抢到商品<---" + productInfo.getProductName());
            }
        } catch (Exception ex) {
            log.error("减库存操作异常->" + ex.getMessage());
            ex.printStackTrace();
        } finally {
            //释放锁
            distributedLock.unlock(key, String.valueOf(time));
        }
    }


    /**
     * 减库存操作
     *
     * @param productId
     * @param productQuantity
     */
    public void decrementProductStoreJedis(Long productId, Integer productQuantity) {
        String key = "dec_store_" + productId + "_jedis_lock";
        // 随机获取访问的用户，模拟不同用户请求
        int index = (int) (Math.random() * userIds.length);
        User user = userService.get(userIds[index]);

        long time = System.currentTimeMillis();
        try {
            // 获取分布式锁,锁定1秒
            int lockMills = 1000;
            //如果加锁失败
            if (jedisDistributedLock.getDistributedLock(jedisUtil.getJedis(), key, String.valueOf(time), lockMills)) {
                ProductInfo productInfo = productInfoService.selectByPrimaryKey(productId);
                // 做幂等性处理
                if (MapUtils.isNotEmpty(killUserIdMaps)
                        && killUserIdMaps.get(user.getId()) != null
                        && killUserIdMaps.get(user.getId()).equals(productId)) {
                    log.warn("用户：" + user.getUsername() + "---已抢到商品：" + productInfo.getProductName() + "，不可以重新领取!");
                    return;
                }
                //判断缓存的商品库存，是否被抢完了
                if (productInfo.getProductStock() > 0 || productInfo.getProductStock() >= productQuantity) {
                    log.info("剩余库存->" + productInfo.getProductStock());
                    // 将秒杀抢到商品的用户存入缓存
                    killUserIdMaps.put(user.getId(), productId);
                    // 减库存操作
                    productInfo.setProductStock(productInfo.getProductStock() - productQuantity);
                    productInfoService.updateByPrimaryKey(productInfo);
                    log.info("用户：" + user.getUsername() + "--->抢到商品：" + productInfo.getProductName());
                } else {
                    log.warn("用户：" + user.getUsername() + "--->未抢到商品<---" + productInfo.getProductName());
                }
            } else {
                log.warn("->获取分布式锁失败，则提示服务器正忙，稍后再试!");
            }

        } catch (Exception ex) {
            log.error("减库存操作异常->" + ex.getMessage());
            ex.printStackTrace();
        } finally {
            //释放锁
            jedisDistributedLock.releaseDistributedLock(jedis, key, String.valueOf(time));
        }
    }
}
