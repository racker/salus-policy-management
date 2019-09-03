/*
 * Copyright 2019 Rackspace US, Inc.
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

import com.rackspace.salus.policy.manage.web.model.MetadataPolicyDTO;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyDTO;
import com.rackspace.salus.telemetry.entities.MonitorPolicy;
import com.rackspace.salus.telemetry.model.MonitorType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
 *
 */
public class PolicyApiClient implements PolicyApi {
  private static final ParameterizedTypeReference<List<MonitorPolicyDTO>> LIST_OF_MONITOR_POLICY = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<MetadataPolicyDTO>> LIST_OF_METADATA_POLICY = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<UUID>> LIST_OF_UUID = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<Map<String, String>> MAP_OF_STRINGS = new ParameterizedTypeReference<>() {};
  private final RestTemplate restTemplate;

  public PolicyApiClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public List<MonitorPolicyDTO> getEffectiveMonitorPolicies(String tenantId) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/monitors/effective/{tenantId}")
        .build(tenantId)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        LIST_OF_MONITOR_POLICY
    ).getBody();
  }

  public List<UUID> getEffectivePolicyMonitorIdsForTenant(String tenantId) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/monitors/effective/{tenantId}/ids")
        .build(tenantId)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        LIST_OF_UUID
    ).getBody();
  }

  public List<MetadataPolicyDTO> getEffectiveMetadataPolicies(String tenantId) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/metadata/effective/{tenantId}")
        .build(tenantId)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        LIST_OF_METADATA_POLICY
    ).getBody();
  }

  public Map<String, String> getEffectiveMetadataMap(String tenantId, MonitorType monitorType) {
    final String uri = UriComponentsBuilder
        .fromPath("/api/admin/policy/metadata/effective/{tenantId}/{monitorType}")
        .build(tenantId, monitorType)
        .toString();

    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        null,
        MAP_OF_STRINGS
    ).getBody();
  }
}
