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

package com.rackspace.salus.policy.manage.web.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.policy.manage.services.TenantManagement;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import com.rackspace.salus.telemetry.web.TenantVerification;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(TenantApiController.class)
/**
 * Tenant Verification is currently not used within Policy Management since only admin
 * api endpoints are available.
 * If public endpoints are created this test ensures that it will work.
 */
public class TenantVerificationTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  TenantManagement tenantManagement;

  @MockBean
  TenantMetadataRepository tenantMetadataRepository;

  /**
   * Verifies tenant verification is not performed for policy admin api requests.
   */
  @Test
  public void testTenantVerification_Ignored() throws Exception {
    String tenantId = RandomStringUtils.randomAlphabetic(10);

    mvc.perform(delete("/api/admin/tenant-metadata/{tenantId}", tenantId)
        // header must be set to trigger tenant verification
        .header(TenantVerification.HEADER_TENANT, tenantId)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(tenantMetadataRepository, never()).existsByTenantId(tenantId);
  }
}
