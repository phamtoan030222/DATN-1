package com.sd20201.datn.core.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zalopay")
public class ZaloPayConfig {
    private String appId;
    private String key1;
    private String key2;
    private String endpoint;
    private String redirectUrl;
    private String callbackUrl;
}