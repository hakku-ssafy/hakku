package com.hakku.storage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.hakku.storage.storage.StorageProperties;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
}
