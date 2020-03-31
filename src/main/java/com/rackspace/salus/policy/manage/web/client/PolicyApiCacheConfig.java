/*
 * Copyright 2020 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.salus.policy.manage.web.client;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the caches used by {@link PolicyApiClient}, so any use of that component should also
 * <code>&#64;Import</code> this config bean.
 */
@Configuration
@EnableConfigurationProperties(PolicyApiCacheProperties.class)
@EnableCaching
public class PolicyApiCacheConfig {

  private final PolicyApiCacheProperties properties;

  @Autowired
  public PolicyApiCacheConfig(PolicyApiCacheProperties properties) {
    this.properties = properties;
  }

  @Bean
  public JCacheManagerCustomizer policyManagementCacheCustomizer() {
    return cacheManager -> {
      cacheManager.createCache("policymgmt_monitor_policies", policiesCacheConfig());
      cacheManager.createCache("policymgmt_monitor_policy_ids", policiesCacheConfig());
      cacheManager.createCache("policymgmt_policy_monitor_ids", policiesCacheConfig());
      cacheManager.createCache("policymgmt_monitor_metadata_policies", metadataCacheConfig());
      cacheManager.createCache("policymgmt_monitor_metadata_map", metadataCacheConfig());
      cacheManager.createCache("policymgmt_zone_metadata", metadataCacheConfig());
    };
  }

  private javax.cache.configuration.Configuration<Object, Object> policiesCacheConfig() {
    return Eh107Configuration.fromEhcacheCacheConfiguration(
        CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class,
            ResourcePoolsBuilder.heap(properties.getPoliciesMaxSize())
        )
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(properties.getTtl()))
    );
  }

  private javax.cache.configuration.Configuration<Object, Object> metadataCacheConfig() {
    return Eh107Configuration.fromEhcacheCacheConfiguration(
        CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class,
            ResourcePoolsBuilder.heap(properties.getMetadataMaxSize())
        )
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(properties.getTtl()))
    );
  }
}
