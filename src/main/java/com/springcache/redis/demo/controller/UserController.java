package com.springcache.redis.demo.controller;

import com.springcache.redis.demo.entity.User;
import com.springcache.redis.demo.response.Result;
import com.springcache.redis.demo.response.ResultUtil;
import com.springcache.redis.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/user")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 更新
     *
     * @param user 用户对象
     * @return 操作结果
     */
    @RequestMapping(value = "/saveOrUpdate", method = RequestMethod.POST)
    public Result<User> saveOrUpdate(@RequestBody User user) {
        User result = userService.saveOrUpdate(user);
        return ResultUtil.success(result);
    }

    /**
     * 获取
     *
     * @param id key值
     * @return 返回结果
     */
    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    public Result<User> get(@PathVariable Long id) {
        User result = userService.get(id);
        return ResultUtil.success(result);
    }

    /**
     * 删除
     *
     * @param id key值
     */
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public void delete(Long id) {
        userService.delete(id);
    }
}
