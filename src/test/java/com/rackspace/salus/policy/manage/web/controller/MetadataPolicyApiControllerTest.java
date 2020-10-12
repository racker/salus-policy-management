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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.policy.manage.services.MonitorMetadataPolicyManagement;
import com.rackspace.salus.policy.manage.web.model.MonitorMetadataPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyUpdate;
import com.rackspace.salus.telemetry.entities.MonitorMetadataPolicy;
import com.rackspace.salus.telemetry.model.MetadataValueType;
import com.rackspace.salus.telemetry.model.MonitorType;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.model.TargetClassName;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@WebMvcTest(MetadataPolicyApiController.class)
@Import({SimpleMeterRegistry.class})
public class MetadataPolicyApiControllerTest {
  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  MonitorMetadataPolicyManagement monitorMetadataPolicyManagement;

  @MockBean
  TenantMetadataRepository tenantMetadataRepository;

  @Test
  public void testGetById() throws Exception {
    MonitorMetadataPolicy policy = (MonitorMetadataPolicy) new MonitorMetadataPolicy()
        .setMonitorType(MonitorType.ping)
        .setValue("test_value")
        .setKey("test_key")
        .setValueType(MetadataValueType.STRING)
        .setTargetClassName(TargetClassName.Monitor)
        .setScope(PolicyScope.GLOBAL)
        .setId(UUID.fromString("5cb19cb3-03e3-4a71-8051-cc7c4bc0c029"))
        .setCreatedTimestamp(Instant.EPOCH)
        .setUpdatedTimestamp(Instant.EPOCH);

    when(monitorMetadataPolicyManagement.getMetadataPolicy(any()))
        .thenReturn(Optional.of(policy));

    mvc.perform(get(
        "/api/admin/policy/metadata/monitor/{uuid}", policy.getId())
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("PolicyApiControllerTest/global_metadata_policy.json"), true));

    verify(monitorMetadataPolicyManagement).getMetadataPolicy(policy.getId());
    verifyNoMoreInteractions(monitorMetadataPolicyManagement);
  }

  @Test
  public void testGetAllMetadataPolicies() throws Exception {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    final List<MonitorMetadataPolicy> listOfPolicies = podamFactory.manufacturePojo(ArrayList.class, MonitorMetadataPolicy.class);

    // Use the APIs default Pageable settings
    int page = 0;
    int pageSize = 20;

    Page<MonitorMetadataPolicy> pageOfPolicies = new PageImpl<>(
        listOfPolicies,
        PageRequest.of(page, pageSize),
        listOfPolicies.size());

    when(monitorMetadataPolicyManagement.getAllMetadataPolicies(any()))
        .thenReturn(pageOfPolicies);

    mvc.perform(get(
        "/api/admin/policy/metadata/monitor", tenantId)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content.*", hasSize(5)))
        .andExpect(jsonPath("$.totalPages", equalTo(1)))
        .andExpect(jsonPath("$.totalElements", equalTo(5)));

    verify(monitorMetadataPolicyManagement).getAllMetadataPolicies(PageRequest.of(page, pageSize));
    verifyNoMoreInteractions(monitorMetadataPolicyManagement);
  }

  @Test
  public void testGetEffectivePoliciesByTenantId() throws Exception {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    final List<MonitorMetadataPolicy> listOfPolicies = podamFactory.manufacturePojo(ArrayList.class, MonitorMetadataPolicy.class);
    when(monitorMetadataPolicyManagement.getEffectiveMetadataPoliciesForTenant(anyString()))
        .thenReturn(listOfPolicies);

    mvc.perform(get(
        "/api/admin/policy/metadata/monitor/effective/{tenantId}", tenantId)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(listOfPolicies)));

    verify(monitorMetadataPolicyManagement).getEffectiveMetadataPoliciesForTenant(tenantId);
    verifyNoMoreInteractions(monitorMetadataPolicyManagement);
  }

  @Test
  public void testCreatePolicy() throws Exception {
    MonitorMetadataPolicy policy = (MonitorMetadataPolicy) new MonitorMetadataPolicy()
        .setMonitorType(MonitorType.ping)
        .setValue("test_value")
        .setKey("test_key")
        .setValueType(MetadataValueType.STRING)
        .setTargetClassName(TargetClassName.Monitor)
        .setScope(PolicyScope.GLOBAL)
        .setId(UUID.fromString("5cb19cb3-03e3-4a71-8051-cc7c4bc0c029"))
        .setCreatedTimestamp(Instant.EPOCH)
        .setUpdatedTimestamp(Instant.EPOCH);

    when(monitorMetadataPolicyManagement.createMetadataPolicy(any()))
        .thenReturn(policy);

    // All we need is a valid create object; doesn't matter what else is set.
    MonitorMetadataPolicyCreate policyCreate = (MonitorMetadataPolicyCreate) new MonitorMetadataPolicyCreate()
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setTargetClassName(TargetClassName.Monitor)
        .setValueType(MetadataValueType.STRING)
        .setKey(RandomStringUtils.randomAlphabetic(10))
        .setValue(RandomStringUtils.randomAlphabetic(10));

    mvc.perform(post(
        "/api/admin/policy/metadata/monitor")
        .content(objectMapper.writeValueAsString(policyCreate))
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("PolicyApiControllerTest/global_metadata_policy.json"), true));

    verify(monitorMetadataPolicyManagement).createMetadataPolicy(policyCreate);
    verifyNoMoreInteractions(monitorMetadataPolicyManagement);
  }

  @Test
  public void testUpdatePolicy() throws Exception {
    MonitorMetadataPolicy policy = (MonitorMetadataPolicy) new MonitorMetadataPolicy()
        .setMonitorType(MonitorType.ping)
        .setValue("test_value")
        .setKey("test_key")
        .setValueType(MetadataValueType.STRING)
        .setTargetClassName(TargetClassName.Monitor)
        .setScope(PolicyScope.GLOBAL)
        .setId(UUID.fromString("5cb19cb3-03e3-4a71-8051-cc7c4bc0c029"))
        .setCreatedTimestamp(Instant.EPOCH)
        .setUpdatedTimestamp(Instant.EPOCH);

    when(monitorMetadataPolicyManagement.updateMetadataPolicy(any(), any()))
        .thenReturn(policy);

    // All we need is a valid update object; doesn't matter what values are set.
    MetadataPolicyUpdate policyUpdate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.STRING)
        .setValue(RandomStringUtils.randomAlphabetic(10));

    UUID id = UUID.randomUUID();
    mvc.perform(put(
        "/api/admin/policy/metadata/monitor/{uuid}", id)
        .content(objectMapper.writeValueAsString(policyUpdate))
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("PolicyApiControllerTest/global_metadata_policy.json"), true));

    verify(monitorMetadataPolicyManagement).updateMetadataPolicy(id, policyUpdate);
    verifyNoMoreInteractions(monitorMetadataPolicyManagement);
  }

  @Test
  public void testRemoveMetadataPolicy() throws Exception {
    UUID id = UUID.randomUUID();
    mvc.perform(delete(
        "/api/admin/policy/metadata/monitor/{uuid}", id)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNoContent());

    verify(monitorMetadataPolicyManagement).removeMetadataPolicy(id);
    verifyNoMoreInteractions(monitorMetadataPolicyManagement);
  }
}
