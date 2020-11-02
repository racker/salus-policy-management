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

import static com.rackspace.salus.policy.manage.web.client.PolicyApiCacheConfig.CACHE_MONITOR_IDS;
import static com.rackspace.salus.policy.manage.web.client.PolicyApiCacheConfig.CACHE_MONITOR_METADATA;
import static com.rackspace.salus.policy.manage.web.client.PolicyApiCacheConfig.CACHE_MONITOR_METADATA_MAP;
import static com.rackspace.salus.policy.manage.web.client.PolicyApiCacheConfig.CACHE_POLICIES;
import static com.rackspace.salus.policy.manage.web.client.PolicyApiCacheConfig.CACHE_POLICY_IDS;
import static com.rackspace.salus.policy.manage.web.client.PolicyApiCacheConfig.CACHE_ZONE_METADATA;

import com.rackspace.salus.policy.manage.web.model.MonitorMetadataPolicyDTO;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyDTO;
import com.rackspace.salus.telemetry.model.MonitorType;
import com.rackspace.salus.telemetry.model.TargetClassName;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This client component provides a small subset of Policy Management REST operations that
 * can be called internally by other microservices in Salus.
 *
 * <p>
 *   It is required that the {@link RestTemplate} provided to this instance has been
 *   configured with the appropriate root URI for locating the policy management service.
 *   The following is an example of a configuration bean that does that:
 * </p>
 *
 * <pre>
  {@literal @}Configuration
  public class RestClientsConfig {

  {@literal @}Bean
  public MonitorApi monitorApi(RestTemplateBuilder restTemplateBuilder) {
    return new PolicyApiClient(
      restTemplateBuilder
        .rootUri("http://localhost:8091")
        .build()
      );
    }
  }
 * </pre>
 * <p>
 *   This component declares the option to cache the results of each operation. To enable caching
 *   <code>&#64;Import</code> {@link PolicyApiCacheConfig} on a config bean declaring the client bean.
 * </p>
 */
public class PolicyApiClient implements PolicyApi {
  private static final ParameterizedTypeReference<List<MonitorPolicyDTO>> LIST_OF_MONITOR_POLICY = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<MonitorMetadataPolicyDTO>> LIST_OF_MONITOR_METADATA_POLICY = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<UUID>> LIST_OF_UUID = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<String>> LIST_OF_STRING = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<Map<String, MonitorMetadataPolicyDTO>> MAP_OF_MONITOR_POLICY = new ParameterizedTypeReference<>() {};
  private final RestTemplate restTemplate;

  public PolicyApiClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @CacheEvict(cacheNames = CACHE_POLICIES, key = "#tenantId",
      condition = "!#useCache", beforeInvocation = true)
  @Cacheable(cacheNames = CACHE_POLICIES, key = "#tenantId",
      condition = "#useCache")
  public List<MonitorPolicyDTO> getEffectiveMonitorPoliciesForTenant(String tenantId, boolean useCache) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/monitors/effective/{tenantId}")
        .buildAndExpand(tenantId)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        LIST_OF_MONITOR_POLICY
    ).getBody();
  }

  @CacheEvict(cacheNames = CACHE_POLICY_IDS, key = "#tenantId",
      condition = "!#useCache", beforeInvocation = true)
  @Cacheable(cacheNames = CACHE_POLICY_IDS, key = "#tenantId",
      condition = "#useCache")
  public List<UUID> getEffectiveMonitorPolicyIdsForTenant(String tenantId, boolean includeNullMonitors, boolean useCache) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/monitors/effective/{tenantId}/policy-ids")
        .queryParam("includeNullMonitors", includeNullMonitors)
        .buildAndExpand(tenantId)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        LIST_OF_UUID
    ).getBody();
  }

  @CacheEvict(cacheNames = CACHE_MONITOR_IDS, key = "#tenantId",
      condition = "!#useCache", beforeInvocation = true)
  @Cacheable(cacheNames = CACHE_MONITOR_IDS, key = "#tenantId",
      condition = "#useCache")
  public List<UUID> getEffectiveMonitorPolicyIdsForTenant(String tenantId, boolean useCache) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/monitors/effective/{tenantId}/monitor-ids")
        .buildAndExpand(tenantId)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        LIST_OF_UUID
    ).getBody();
  }

  @CacheEvict(cacheNames = CACHE_MONITOR_METADATA, key = "#tenantId",
      condition = "!#useCache", beforeInvocation = true)
  @Cacheable(cacheNames = CACHE_MONITOR_METADATA, key = "#tenantId",
      condition = "#useCache")
  public List<MonitorMetadataPolicyDTO> getEffectiveMonitorMetadataPolicies(
      String tenantId, boolean useCache) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/metadata/monitor/effective/{tenantId}")
        .buildAndExpand(tenantId)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        LIST_OF_MONITOR_METADATA_POLICY
    ).getBody();
  }

  @CacheEvict(cacheNames = CACHE_MONITOR_METADATA_MAP, key = "{#tenantId, #className, #monitorType}",
      condition = "!#useCache", beforeInvocation = true)
  @Cacheable(cacheNames = CACHE_MONITOR_METADATA_MAP, key = "{#tenantId, #className, #monitorType}",
      condition = "#useCache")
  public Map<String, MonitorMetadataPolicyDTO> getEffectiveMonitorMetadataMap(
      String tenantId, TargetClassName className, MonitorType monitorType, boolean useCache) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/metadata/monitor/effective/{tenantId}/{className}/{monitorType}")
        .buildAndExpand(tenantId, className, monitorType)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        MAP_OF_MONITOR_POLICY
    ).getBody();
  }

  @CacheEvict(cacheNames = CACHE_ZONE_METADATA, key = "#region", condition = "!#useCache",
      beforeInvocation = true)
  @Cacheable(cacheNames = CACHE_ZONE_METADATA, key = "#region", condition = "#useCache")
  public List<String> getDefaultMonitoringZones(String region, boolean useCache) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/metadata/zones/{region}")
        .buildAndExpand(region)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        LIST_OF_STRING
    ).getBody();
  }
}
