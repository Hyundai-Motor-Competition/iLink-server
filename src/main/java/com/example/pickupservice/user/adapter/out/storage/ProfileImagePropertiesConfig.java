package com.example.pickupservice.user.adapter.out.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProfileImageProperties.class)
class ProfileImagePropertiesConfig {
}
