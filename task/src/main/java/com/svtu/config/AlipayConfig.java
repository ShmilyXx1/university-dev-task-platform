package com.svtu.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝沙箱配置
 * 参数来源：application.properties 中 alipay.* 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String appId;             // 沙箱 APPID
    private String appPrivateKey;      // 应用私钥
    private String alipayPublicKey;    // 支付宝公钥
    private String gateway;            // 沙箱网关
    private String notifyUrl;          // 异步通知地址
    private String returnUrl;          // 支付完成同步跳转地址（前端结果页）

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                gateway,
                appId,
                appPrivateKey,
                "json",
                "UTF-8",
                alipayPublicKey,
                "RSA2"
        );
    }
}
