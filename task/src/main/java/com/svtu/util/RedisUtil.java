package com.svtu.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类（适配JWT令牌管理）
 * 功能：缓存令牌、删除令牌、检查令牌是否存在、设置过期时间
 *      分布式锁（SETNX）、未读消息计数（Hash HINCRBY）
 */
@Component
public class RedisUtil {

    @Autowired
    public RedisTemplate<String, Object> redisTemplate;

    /**
     * 纯字符串模板：用于分布式锁与未读计数。
     * 主模板 redisTemplate 的 value 用 GenericJackson2JsonRedisSerializer 序列化，
     * 会让 Hash 计数值带上类型信息，导致 HINCRBY 报错；故计数/锁走 StringRedisTemplate。
     */
    @Autowired
    public StringRedisTemplate stringRedisTemplate;

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

    // ==================== 分布式锁（SETNX + 过期） ====================

    /**
     * 尝试获取分布式锁（SET key value NX EX）。
     * 成功返回 true，已被占用返回 false。
     * @param key 锁键（建议：业务:lock:id）
     * @param value 持有者标识（建议用 userId / UUID，释放时校验防误删）
     * @param seconds 锁过期时间（秒，兜底防止死锁）
     */
    public boolean tryLock(String key, String value, long seconds) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(key, value, seconds, TimeUnit.SECONDS));
    }

    /**
     * 释放分布式锁（仅当 value 匹配才删除，避免误删他人持有的锁）。
     */
    public void unlock(String key, String value) {
        String cur = stringRedisTemplate.opsForValue().get(key);
        if (value.equals(cur)) {
            stringRedisTemplate.delete(key);
        }
    }

    // ==================== Hash 未读消息计数 ====================

    /**
     * Hash 自增（收消息时 +1）。要求 Hash field 的值为数字字符串，故走 StringRedisTemplate。
     * @param key 如 unread:{接收者userId}
     * @param field 如 {发送者userId}
     */
    public void hIncrBy(String key, String field, long delta) {
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
        hashOps.increment(key, field, delta);
    }

    /**
     * Hash 设置某字段值（标记已读时置 0）。
     */
    public void hSet(String key, String field, String value) {
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
        hashOps.put(key, field, value);
    }

    /**
     * Hash 获取全部字段（查询某人所有会话的未读数）。
     */
    public Map<String, String> hGetAll(String key) {
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
        return hashOps.entries(key);
    }
}
