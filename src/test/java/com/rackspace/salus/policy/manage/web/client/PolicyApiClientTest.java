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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.policy.manage.web.model.MonitorMetadataPolicyDTO;
import com.rackspace.salus.telemetry.model.MetadataValueType;
import com.rackspace.salus.telemetry.model.MonitorType;
import com.rackspace.salus.telemetry.model.TargetClassName;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;


/**
 * Since caching is duplicated across all endpoints, only getEffectiveMonitorMetadataMap
 * is tested here.  Those tests infer the others will also work (as long as the @Cacheable
 * config was duplicated correctly)
 */
@RunWith(SpringRunner.class)
@RestClientTest
@AutoConfigureCache(cacheProvider = CacheType.JCACHE)
public class PolicyApiClientTest {

  @TestConfiguration
  @Import(PolicyApiCacheConfig.class)
  public static class ExtraTestConfig {
    @Bean
    public PolicyApi policyApiClient(RestTemplateBuilder restTemplateBuilder) {
      return new PolicyApiClient(restTemplateBuilder.build());
    }
  }
  @Autowired
  MockRestServiceServer mockServer;

  @Autowired
  PolicyApi policyApiClient;

  @MockBean
  TenantMetadataRepository tenantMetadataRepository;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Tests the same request multiple times with the cache enabled.
   *
   * @throws JsonProcessingException
   */
  @Test
  public void testGetEffectiveMonitorMetadataMapWithCache() throws JsonProcessingException {
    Map<String, MonitorMetadataPolicyDTO> expectedPolicy = Map.of(
        "count", (MonitorMetadataPolicyDTO) new MonitorMetadataPolicyDTO()
            .setKey("count")
            .setValueType(MetadataValueType.INT)
            .setValue("63"),
        "pingInterval", (MonitorMetadataPolicyDTO) new MonitorMetadataPolicyDTO()
            .setKey("pingInterval")
            .setValueType(MetadataValueType.DURATION)
            .setValue("PT2S"));

    String tenantId = "hybrid:123456";

    mockServer.expect(ExpectedCount.once(),
        requestTo(String.format(
            "/api/admin/policy/metadata/monitor/effective/%s/RemotePlugin/ping", tenantId)))
        .andRespond(withSuccess(
            objectMapper.writeValueAsString(expectedPolicy), MediaType.APPLICATION_JSON
        ));

    Map<String, MonitorMetadataPolicyDTO> policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, true);

    assertThat(policies, equalTo(expectedPolicy));

    // running the same request again should return the same result from the cache
    policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, true);

    assertThat(policies, equalTo(expectedPolicy));
    mockServer.verify();
  }

  /**
   * Tests the same request multiple times with the cache disabled.
   *
   * @throws JsonProcessingException
   */
  @Test
  public void testGetEffectiveMonitorMetadataMapNoCache() throws JsonProcessingException {
    Map<String, MonitorMetadataPolicyDTO> expectedPolicy = Map.of(
        "count", (MonitorMetadataPolicyDTO) new MonitorMetadataPolicyDTO()
            .setKey("count")
            .setValueType(MetadataValueType.INT)
            .setValue("63"),
        "pingInterval", (MonitorMetadataPolicyDTO) new MonitorMetadataPolicyDTO()
            .setKey("pingInterval")
            .setValueType(MetadataValueType.DURATION)
            .setValue("PT2S"));

    String tenantId = "hybrid:123456";

    mockServer.expect(ExpectedCount.twice(),
        requestTo(String.format(
            "/api/admin/policy/metadata/monitor/effective/%s/RemotePlugin/ping", tenantId)))
        .andRespond(withSuccess(
            objectMapper.writeValueAsString(expectedPolicy), MediaType.APPLICATION_JSON
        ));

    Map<String, MonitorMetadataPolicyDTO> policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, false);

    assertThat(policies, equalTo(expectedPolicy));

    // running the same request again should bypass the cache and perform a full request
    policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, false);

    assertThat(policies, equalTo(expectedPolicy));
    mockServer.verify();
  }

  /**
   * Tests a request for ping metadata multiple times with a mixture of cache settings.
   * Then tests requests for http metadata to verify it does not reuse the same cache entry created
   * by the ping requests.
   *
   * @throws JsonProcessingException
   */
  @Test
  public void testGetEffectiveMonitorMetadataMapMultiRequest() throws JsonProcessingException {
    Map<String, MonitorMetadataPolicyDTO> expectedPingPolicy = Map.of(
        "count", (MonitorMetadataPolicyDTO) new MonitorMetadataPolicyDTO()
            .setKey("count")
            .setValueType(MetadataValueType.INT)
            .setValue("63"),
        "pingInterval", (MonitorMetadataPolicyDTO) new MonitorMetadataPolicyDTO()
            .setKey("pingInterval")
            .setValueType(MetadataValueType.DURATION)
            .setValue("PT2S"));

    Map<String, MonitorMetadataPolicyDTO> expectedHttpPolicy = Map.of(
        "timeout", (MonitorMetadataPolicyDTO) new MonitorMetadataPolicyDTO()
            .setKey("timeout")
            .setValueType(MetadataValueType.DURATION)
            .setValue("PT1M"));

    String tenantId = "hybrid:123456";

    // only one of the three requests will hit the cache
    mockServer.expect(ExpectedCount.twice(),
        requestTo(String.format(
            "/api/admin/policy/metadata/monitor/effective/%s/RemotePlugin/ping", tenantId)))
        .andRespond(withSuccess(
            objectMapper.writeValueAsString(expectedPingPolicy), MediaType.APPLICATION_JSON
        ));

    // first request will populate the cache
    Map<String, MonitorMetadataPolicyDTO> policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, true);

    assertThat(policies, equalTo(expectedPingPolicy));

    // make the same request using the cache
    policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, true);

    assertThat(policies, equalTo(expectedPingPolicy));

    // make the same request bypassing the cache
    policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, false);

    assertThat(policies, equalTo(expectedPingPolicy));

    mockServer.verify();
    mockServer.reset(); // allows us to set a new `expect`

    // same request with a different monitor type should not query the existing cache
    // all but the first request will hit the cache
    mockServer.expect(ExpectedCount.once(),
        requestTo(String.format(
            "/api/admin/policy/metadata/monitor/effective/%s/RemotePlugin/http", tenantId)))
        .andRespond(withSuccess(
            objectMapper.writeValueAsString(expectedHttpPolicy), MediaType.APPLICATION_JSON
        ));

    // Run the request for a different monitor type 3 times using the cache
    // the same result will be returned each time
    policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.http, true);
    assertThat(policies, equalTo(expectedHttpPolicy));

    policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.http, true);
    assertThat(policies, equalTo(expectedHttpPolicy));

    policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.http, true);
    assertThat(policies, equalTo(expectedHttpPolicy));

    mockServer.verify();
  }

}
