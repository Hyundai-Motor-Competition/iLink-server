package com.example.pickupservice.pickup.application.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PickupProperties.class)
class PickupPropertiesConfig {
}
