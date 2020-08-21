package com.springcache.redis.demo.response;

import lombok.Data;

@Data
public class ErrorDto {

    private String code;

    private String msg;

    private String innerMsg;

}
