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

package com.rackspace.salus.policy.manage.web.controller;

import static com.rackspace.salus.test.JsonTestUtils.readContent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyDTO;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyUpdate;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.entities.MonitorPolicy;
import com.rackspace.salus.policy.manage.services.MonitorPolicyManagement;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@WebMvcTest(MonitorPolicyApiController.class)
@Import({SimpleMeterRegistry.class})
public class MonitorPolicyApiControllerTest {

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  MonitorPolicyManagement monitorPolicyManagement;

  @MockBean
  TenantMetadataRepository tenantMetadataRepository;

  @Test
  public void testGetById() throws Exception {
    MonitorPolicy policy = (MonitorPolicy) new MonitorPolicy()
        .setMonitorTemplateId(UUID.fromString("32e3ac07-5a80-4d56-8519-f66eb66ec6b6"))
        .setName("Test Name")
        .setScope(PolicyScope.GLOBAL)
        .setId(UUID.fromString("c0f88d34-2833-4ebb-926c-3601795901f9"))
        .setCreatedTimestamp(Instant.EPOCH)
        .setUpdatedTimestamp(Instant.EPOCH);

    when(monitorPolicyManagement.getMonitorPolicy(any()))
        .thenReturn(Optional.of(policy));

    mvc.perform(get(
        "/api/admin/policy/monitors/{uuid}", policy.getId())
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("PolicyApiControllerTest/global_monitor_policy.json"), true));

    verify(monitorPolicyManagement).getMonitorPolicy(policy.getId());
    verifyNoMoreInteractions(monitorPolicyManagement);
  }

  @Test
  public void testGetEffectivePoliciesByTenantId() throws Exception {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    final List<MonitorPolicy> listOfPolicies = podamFactory.manufacturePojo(ArrayList.class, MonitorPolicy.class);

    List<MonitorPolicyDTO> listOfPoliciesDTO = listOfPolicies.parallelStream().map(MonitorPolicyDTO::new).collect(Collectors.toList());
    when(monitorPolicyManagement.getEffectiveMonitorPoliciesForTenant(anyString()))
        .thenReturn(listOfPolicies);

    mvc.perform(get(
        "/api/admin/policy/monitors/effective/{tenantId}", tenantId)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(listOfPoliciesDTO)));

    verify(monitorPolicyManagement).getEffectiveMonitorPoliciesForTenant(tenantId);
    verifyNoMoreInteractions(monitorPolicyManagement);
  }

  @Test
  public void testGetEffectivePolicyIdsByTenantId() throws Exception {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    final List<UUID> listOfPolicyIds = podamFactory.manufacturePojo(ArrayList.class, UUID.class);
    when(monitorPolicyManagement.getEffectiveMonitorPolicyIdsForTenant(anyString(), anyBoolean()))
        .thenReturn(listOfPolicyIds);

    mvc.perform(get(
        "/api/admin/policy/monitors/effective/{tenantId}/policy-ids", tenantId)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(listOfPolicyIds)));

    verify(monitorPolicyManagement).getEffectiveMonitorPolicyIdsForTenant(tenantId, true);
    verifyNoMoreInteractions(monitorPolicyManagement);
  }

  @Test
  public void testGetEffectivePolicyIdsByTenantId_excludeNull() throws Exception {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    final List<UUID> listOfPolicyIds = podamFactory.manufacturePojo(ArrayList.class, UUID.class);
    when(monitorPolicyManagement.getEffectiveMonitorPolicyIdsForTenant(anyString(), anyBoolean()))
        .thenReturn(listOfPolicyIds);

    mvc.perform(get(
        "/api/admin/policy/monitors/effective/{tenantId}/policy-ids", tenantId)
        .queryParam("includeNullMonitors", "false")
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(listOfPolicyIds)));

    verify(monitorPolicyManagement).getEffectiveMonitorPolicyIdsForTenant(tenantId, false);
    verifyNoMoreInteractions(monitorPolicyManagement);
  }

  @Test
  public void testCreatePolicy() throws Exception {
    MonitorPolicy policy = getPolicy();

    when(monitorPolicyManagement.createMonitorPolicy(any()))
        .thenReturn(policy);

    // All we need is a valid create object; doesn't matter what else is set.
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorTemplateId(UUID.fromString("32e3ac07-5a80-4d56-8519-f66eb66ec6b6"));

    mvc.perform(post(
        "/api/admin/policy/monitors")
        .content(objectMapper.writeValueAsString(policyCreate))
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("PolicyApiControllerTest/global_monitor_policy.json"), true));

    verify(monitorPolicyManagement).createMonitorPolicy(policyCreate);
    verifyNoMoreInteractions(monitorPolicyManagement);
  }

  private MonitorPolicy getPolicy() {
    return (MonitorPolicy) new MonitorPolicy()
        .setMonitorTemplateId(UUID.fromString("32e3ac07-5a80-4d56-8519-f66eb66ec6b6"))
        .setName("Test Name")
        .setScope(PolicyScope.GLOBAL)
        .setId(UUID.fromString("c0f88d34-2833-4ebb-926c-3601795901f9"))
        .setCreatedTimestamp(Instant.EPOCH)
        .setUpdatedTimestamp(Instant.EPOCH);
  }

  @Test
  public void testUpdatePolicy() throws Exception {
    MonitorPolicy policy = getPolicy();

    MonitorPolicyUpdate policyUpdate = new MonitorPolicyUpdate()
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope("test");

    when(monitorPolicyManagement.updateMonitorPolicy(any(), any()))
        .thenReturn(policy);

    mvc.perform(put(
        "/api/admin/policy/monitors/{uuid}", policy.getId())
        .content(objectMapper.writeValueAsString(policyUpdate))
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("PolicyApiControllerTest/global_monitor_policy.json"), true));

    verify(monitorPolicyManagement).updateMonitorPolicy(policy.getId(), policyUpdate);
    verifyNoMoreInteractions(monitorPolicyManagement);
  }

  @Test
  public void testRemoveMonitorPolicy() throws Exception {
    UUID id = UUID.randomUUID();
    mvc.perform(delete(
        "/api/admin/policy/monitors/{uuid}", id)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNoContent());

    verify(monitorPolicyManagement).removeMonitorPolicy(id);
    verifyNoMoreInteractions(monitorPolicyManagement);
  }
}
