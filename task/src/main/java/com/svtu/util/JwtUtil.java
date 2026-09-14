package com.svtu.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 * 功能：生成令牌、解析令牌、验证令牌、提取用户信息
 */
@Slf4j
@Component
public class JwtUtil {

    // 从配置文件读取JWT密钥（建议至少32位）
    @Value("${jwt.secret:default-secret-key-32bit-1234567890}")
    private String secret;

    // 从配置文件读取令牌过期时间（毫秒）
    @Value("${jwt.expire}")
    private long expireTime;

    /**
     * 生成JWT令牌（含自定义载荷）
     * @param userDetails 用户信息（Spring Security的UserDetails）
     * @return JWT令牌
     */
    public String generateToken(UserDetails userDetails,Integer userId) {
        // 1. 构建自定义载荷（可添加用户ID、角色等信息）
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", userDetails.getUsername());
        claims.put("authorities", userDetails.getAuthorities());
        claims.put("userId",userId);

        // 2. 生成令牌
        return Jwts.builder()
                // 设置载荷
                .setClaims(claims)
                // 设置主题（用户名）
                .setSubject(userDetails.getUsername())
                // 设置签发时间
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // 设置过期时间
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                // 设置签名密钥（使用HS256算法）
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                // 压缩并序列化
                .compact();
    }

    /**
     * 从令牌中提取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中提取指定载荷
     * @param token JWT令牌
     * @param claimsResolver 载荷解析器
     * @return 解析结果
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 验证令牌是否有效（用户名匹配 + 未过期）
     * @param token JWT令牌
     * @param userDetails 用户信息
     * @return true=有效，false=无效
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * 检查令牌是否过期
     * @param token JWT令牌
     * @return true=过期，false=未过期
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 提取令牌过期时间
     * @param token JWT令牌
     * @return 过期时间
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 解析令牌所有载荷（会验证签名，无效则抛异常）
     * @param token JWT令牌
     * @return 载荷信息
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (MalformedJwtException e) {
            log.error("JWT令牌格式错误", e);
            throw new RuntimeException("令牌格式错误");
        } catch (ExpiredJwtException e) {
            log.error("JWT令牌已过期", e);
            throw new RuntimeException("令牌已过期");
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT令牌", e);
            throw new RuntimeException("不支持的令牌类型");
        } catch (IllegalArgumentException e) {
            log.error("JWT令牌为空或无效", e);
            throw new RuntimeException("令牌为空或无效");
        } catch (SignatureException e) {
            log.error("JWT签名验证失败", e);
            throw new RuntimeException("令牌签名验证失败");
        }
    }

    /**
     * 获取签名密钥（转换为SecretKey对象，避免密钥长度不足问题）
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        // 将字符串密钥转换为符合HS256要求的SecretKey
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
