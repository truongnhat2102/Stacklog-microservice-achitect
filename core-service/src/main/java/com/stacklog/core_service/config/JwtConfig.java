package com.stacklog.core_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.stacklog.core_service.utils.jwt.JwtProperties;


@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {    


}
