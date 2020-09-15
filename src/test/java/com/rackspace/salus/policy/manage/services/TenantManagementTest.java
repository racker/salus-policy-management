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

package com.rackspace.salus.policy.manage.services;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.rackspace.salus.policy.manage.config.DatabaseConfig;
import com.rackspace.salus.policy.manage.web.model.TenantMetadataCU;
import com.rackspace.salus.telemetry.entities.TenantMetadata;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.TenantPolicyChangeEvent;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import com.rackspace.salus.test.EnableTestContainersDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@EnableTestContainersDatabase
@DataJpaTest(showSql = false)
@Import({TenantManagement.class, DatabaseConfig.class,
    MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
public class TenantManagementTest {

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Autowired
  TenantManagement tenantManagement;

  @Autowired
  TenantMetadataRepository tenantMetadataRepository;

  @MockBean
  PolicyEventProducer policyEventProducer;

  private TenantMetadata defaultMetadata;

  @Before
  public void setup() {
    defaultMetadata = tenantMetadataRepository.save(podamFactory.manufacturePojo(TenantMetadata.class));
  }

  @Test
  public void testGetMetadata() {
    TenantMetadata original = tenantMetadataRepository.save(podamFactory.manufacturePojo(TenantMetadata.class));
    Optional<TenantMetadata> metadata = tenantManagement.getMetadata(original.getTenantId());
    assertTrue(metadata.isPresent());
    assertThat(metadata.get(), equalTo(original));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testGetAccountTypeByTenant() {
    String accountType = tenantManagement.getAccountTypeByTenant(defaultMetadata.getTenantId());
    assertThat(accountType, notNullValue());
    assertThat(accountType, equalTo(defaultMetadata.getAccountType()));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testGetAccountTypeByTenant_accountDoesntExist() {
    String accountType = tenantManagement.getAccountTypeByTenant(RandomStringUtils.randomAlphabetic(10));
    assertThat(accountType, nullValue());

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testCreateTenantMetadata() {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    TenantMetadataCU create = podamFactory.manufacturePojo(TenantMetadataCU.class);

    TenantMetadata metadata = tenantManagement.createMetadata(tenantId, create);
    assertThat(metadata, notNullValue());
    assertThat(metadata.getId(), notNullValue());
    assertThat(metadata.getTenantId(), equalTo(tenantId));
    assertThat(metadata.getAccountType(), equalTo(create.getAccountType()));
    assertThat(metadata.getMetadata(), equalTo(create.getMetadata()));

    verify(policyEventProducer).sendTenantChangeEvent(
        new TenantPolicyChangeEvent()
            .setTenantId(tenantId));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testUpdateTenantMetadata() {
    TenantMetadata original = tenantMetadataRepository.save(podamFactory.manufacturePojo(TenantMetadata.class));

    Map<String, String> newMetadata = new HashMap<>(original.getMetadata());
    newMetadata.put("new", "value");

    TenantMetadataCU update = new TenantMetadataCU()
        .setAccountType("updated AccountType")
        .setMetadata(newMetadata);

    TenantMetadata metadata = tenantManagement.updateMetadata(original.getTenantId(), update);
    assertThat(metadata, notNullValue());
    assertThat(metadata.getId(), equalTo(original.getId()));
    assertThat(metadata.getTenantId(), equalTo(original.getTenantId()));
    assertThat(metadata.getAccountType(), equalTo(update.getAccountType()));
    assertThat(metadata.getMetadata(), equalTo(update.getMetadata()));

    verify(policyEventProducer).sendTenantChangeEvent(
        new TenantPolicyChangeEvent()
            .setTenantId(original.getTenantId()));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testRemoveTenantMetadata() {
    TenantMetadata original = tenantMetadataRepository.save(podamFactory.manufacturePojo(TenantMetadata.class));
    tenantManagement.removeTenantMetadata(original.getTenantId());
    Optional<TenantMetadata> metadata = tenantManagement.getMetadata(original.getTenantId());
    assertTrue(metadata.isEmpty());

    verify(policyEventProducer).sendTenantChangeEvent(
        new TenantPolicyChangeEvent()
            .setTenantId(original.getTenantId()));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test(expected = AlreadyExistsException.class)
  public void testCreateTenantMetadata_alreadyExists() {
    String tenantId = "aaaaaa";
    TenantMetadataCU create = podamFactory.manufacturePojo(TenantMetadataCU.class);

    TenantMetadata tenantMetadata = podamFactory.manufacturePojo(TenantMetadata.class);
    tenantMetadata.setTenantId("aaaaaa");
    tenantMetadataRepository.save(tenantMetadata);

    tenantManagement.createMetadata(tenantId, create);
  }

  @Test
  public void testUpdateTenantMetadata_tenantNotFound() {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    TenantMetadataCU update = podamFactory.manufacturePojo(TenantMetadataCU.class);

    TenantMetadata metadata = tenantManagement.updateMetadata(tenantId, update);
    assertThat(metadata, notNullValue());
    assertThat(metadata.getId(), notNullValue());
    assertThat(metadata.getTenantId(), equalTo(tenantId));
    assertThat(metadata.getAccountType(), equalTo(update.getAccountType()));
    assertThat(metadata.getMetadata(), equalTo(update.getMetadata()));

    verify(policyEventProducer).sendTenantChangeEvent(
        new TenantPolicyChangeEvent()
            .setTenantId(tenantId));

    verifyNoMoreInteractions(policyEventProducer);
  }
}
