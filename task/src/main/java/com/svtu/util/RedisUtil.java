package com.svtu.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类（适配JWT令牌管理）
 * 功能：缓存令牌、删除令牌、检查令牌是否存在、设置过期时间
 */
@Component
public class RedisUtil {

    @Autowired
    public RedisTemplate<String, Object> redisTemplate;

    // ==================== JWT令牌相关操作 ====================

    /**
     * 将JWT令牌存入Redis（设置过期时间，与令牌过期时间一致）
     * @param key 键（建议格式：token:username:xxx）
     * @param token JWT令牌
     * @param expireTime 过期时间（秒）
     */
    public void setTokenAndTime(String key, String token, long expireTime) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        operations.set(key, token, expireTime, TimeUnit.SECONDS);
    }

    public void setToken(String key, Object token) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        operations.set(key, token);
    }

    /**
     * 从Redis获取令牌
     * @param key 键
     * @return 令牌（null=不存在）
     */
    public String getToken(String key) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        Object value = operations.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 检查令牌是否存在于Redis（用于黑名单校验）
     * @param key 键
     * @return true=存在，false=不存在
     */
    public boolean hasToken(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除Redis中的令牌（比如用户登出时）
     * @param key 键
     * @return true=删除成功，false=删除失败
     */
    public boolean deleteToken(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // ==================== 通用Redis操作（可选扩展） ====================

    /**
     * 通用设置缓存（带过期时间）
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间（秒）
     */
    public void set(String key, Object value, long expireTime) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        operations.set(key, value, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 通用获取缓存
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        return operations.get(key);
    }

    /**
     * 通用删除缓存
     * @param key 键
     * @return true=成功，false=失败
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }
}
