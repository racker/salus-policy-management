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
import com.rackspace.salus.telemetry.entities.TenantMetadata;
import com.rackspace.salus.policy.manage.services.TenantManagement;
import com.rackspace.salus.policy.manage.web.model.TenantMetadataCU;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import edu.emory.mathcs.backport.java.util.Collections;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import org.springframework.http.MediaType;

@RunWith(SpringRunner.class)
@WebMvcTest(TenantApiController.class)
public class TenantApiControllerTest {

  // A timestamp to be used in tests that translates to "1970-01-02T03:46:40Z"
  private static final Instant DEFAULT_TIMESTAMP = Instant.ofEpochSecond(100000);

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  TenantManagement tenantManagement;

  @MockBean
  TenantMetadataRepository tenantMetadataRepository;

  @Test
  public void testGetMetadata() throws Exception {
    TenantMetadata metadata = new TenantMetadata()
        .setId(UUID.fromString("09867b47-2da6-4100-9366-8facf499285a"))
        .setTenantId("MyTenantId")
        .setAccountType("MyAccountType")
        .setMetadata(Collections.singletonMap("dummy", "value"))
        .setCreatedTimestamp(DEFAULT_TIMESTAMP)
        .setUpdatedTimestamp(DEFAULT_TIMESTAMP);

    when(tenantManagement.getMetadata(any()))
        .thenReturn(Optional.of(metadata));

    mvc.perform(get(
        "/api/admin/tenant-metadata/{tenantId}", metadata.getTenantId())
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("TenantApiControllerTest/basic_tenant_metadata.json"), true));

    verify(tenantManagement).getMetadata(metadata.getTenantId());
    verifyNoMoreInteractions(tenantManagement);
  }

  @Test
  public void testUpdateMetaData() throws Exception {
    TenantMetadata metadata = new TenantMetadata()
        .setId(UUID.fromString("09867b47-2da6-4100-9366-8facf499285a"))
        .setTenantId("MyTenantId")
        .setAccountType("MyAccountType")
        .setMetadata(Collections.singletonMap("dummy", "value"))
        .setCreatedTimestamp(DEFAULT_TIMESTAMP)
        .setUpdatedTimestamp(DEFAULT_TIMESTAMP);

    when(tenantManagement.updateMetaData(anyString(), any()))
        .thenReturn(metadata);

    TenantMetadataCU createOrUpdate = podamFactory.manufacturePojo(TenantMetadataCU.class);
    mvc.perform(put(
        "/api/admin/tenant-metadata/{tenantId}", metadata.getTenantId())
        .content(objectMapper.writeValueAsString(createOrUpdate))
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("TenantApiControllerTest/basic_tenant_metadata.json"), true));

    verify(tenantManagement).updateMetaData(metadata.getTenantId(), createOrUpdate);
    verifyNoMoreInteractions(tenantManagement);

  }

  @Test
  public void testRemoveMetadata() throws Exception {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    mvc.perform(delete(
        "/api/admin/tenant-metadata/{tenantId}", tenantId)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNoContent());

    verify(tenantManagement).removeTenantMetadata(tenantId);
    verifyNoMoreInteractions(tenantManagement);
  }

  @Test
  public void testCreateMetaData() throws Exception {
    TenantMetadata metadata = new TenantMetadata()
        .setId(UUID.fromString("09867b47-2da6-4100-9366-8facf499285a"))
        .setTenantId("MyTenantId")
        .setAccountType("MyAccountType")
        .setMetadata(Collections.singletonMap("dummy", "value"))
        .setCreatedTimestamp(DEFAULT_TIMESTAMP)
        .setUpdatedTimestamp(DEFAULT_TIMESTAMP);

    when(tenantManagement.createMetaData(anyString(), any()))
        .thenReturn(metadata);

    TenantMetadataCU createOrUpdate = podamFactory.manufacturePojo(TenantMetadataCU.class);
    createOrUpdate.setTenantId("MyTenantId");
    mvc.perform(post(
        "/api/admin/tenant-metadata")
        .content(objectMapper.writeValueAsString(createOrUpdate))
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            readContent("TenantApiControllerTest/basic_tenant_metadata.json"), true));

    verify(tenantManagement).createMetaData(metadata.getTenantId(), createOrUpdate);
    verifyNoMoreInteractions(tenantManagement);

  }
}
