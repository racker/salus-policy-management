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
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;


@RunWith(SpringRunner.class)
@RestClientTest
public class PolicyApiClientTest {

  @TestConfiguration
  public static class ExtraTestConfig {
    @Bean
    public PolicyApiClient policyApiClient(RestTemplateBuilder restTemplateBuilder) {
      return new PolicyApiClient(restTemplateBuilder.build());
    }
  }
  @Autowired
  MockRestServiceServer mockServer;

  @Autowired
  PolicyApiClient policyApiClient;

  private static final ObjectMapper objectMapper = new ObjectMapper();

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

    String tenantId = RandomStringUtils.randomAlphanumeric(10);

    mockServer.expect(ExpectedCount.once(),
        requestTo(String.format(
            "/api/admin/policy/metadata/monitor/effective/%s/RemotePlugin/ping", tenantId)))
        .andRespond(withSuccess(
            objectMapper.writeValueAsString(expectedPolicy), MediaType.APPLICATION_JSON
        ));

    Map<String, MonitorMetadataPolicyDTO> policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, true);

    assertThat(policies, equalTo(expectedPolicy));

    // running the same request again should trigger bypass the cache and perform a full request
    policies = policyApiClient.getEffectiveMonitorMetadataMap(
        tenantId, TargetClassName.RemotePlugin, MonitorType.ping, true);

    assertThat(policies, equalTo(expectedPolicy));
    mockServer.verify();
  }

}
