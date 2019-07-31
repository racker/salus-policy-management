/*
 *
 *  * Copyright 2019 Rackspace US, Inc.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.rackspace.salus.policy.manage.services;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.rackspace.salus.policy.manage.entities.TenantMetadata;
import com.rackspace.salus.policy.manage.repositories.TenantMetadataRepository;
import com.rackspace.salus.policy.manage.web.model.TenantMetadataCU;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@DataJpaTest(showSql = false)
@Import({TenantManagement.class})
public class TenantManagementTest {

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Autowired
  TenantManagement tenantManagement;

  @Autowired
  TenantMetadataRepository tenantMetadataRepository;

  private TenantMetadata defaultMetadata;

  @Before
  public void setup() {
    defaultMetadata = tenantMetadataRepository.save(podamFactory.manufacturePojo(TenantMetadata.class));
  }

  @Test
  public void testGetMetadata() {
    TenantMetadata original = tenantMetadataRepository.save(podamFactory.manufacturePojo(TenantMetadata.class));
    tenantManagement.getMetadata(original.getTenantId());
    Optional<TenantMetadata> metadata = tenantManagement.getMetadata(original.getTenantId());
    assertTrue(metadata.isPresent());
    assertThat(metadata.get(), equalTo(original));
  }

  @Test
  public void testGetAccountTypeByTenant() {
    String accountType = tenantManagement.getAccountTypeByTenant(defaultMetadata.getTenantId());
    assertThat(accountType, notNullValue());
    assertThat(accountType, equalTo(defaultMetadata.getAccountType()));
  }

  @Test
  public void testGetAccountTypeByTenant_accountDoesntExist() {
    String accountType = tenantManagement.getAccountTypeByTenant(RandomStringUtils.randomAlphabetic(10));
    assertThat(accountType, nullValue());
  }

  @Test
  public void testCreateTenantMetadata() {
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    TenantMetadataCU create = podamFactory.manufacturePojo(TenantMetadataCU.class);

    TenantMetadata metadata = tenantManagement.upsertTenantMetadata(tenantId, create);
    assertThat(metadata, notNullValue());
    assertThat(metadata.getId(), notNullValue());
    assertThat(metadata.getTenantId(), equalTo(tenantId));
    assertThat(metadata.getAccountType(), equalTo(create.getAccountType()));
    assertThat(metadata.getMetadata(), equalTo(create.getMetadata()));
  }

  @Test
  public void testUpdateTenantMetadata() {
    TenantMetadata original = tenantMetadataRepository.save(podamFactory.manufacturePojo(TenantMetadata.class));

    Map<String, String> newMetadata = new HashMap<>(original.getMetadata());
    newMetadata.put("new", "value");

    TenantMetadataCU update = new TenantMetadataCU()
        .setAccountType("updated AccountType")
        .setMetadata(newMetadata);

    TenantMetadata metadata = tenantManagement.upsertTenantMetadata(original.getTenantId(), update);
    assertThat(metadata, notNullValue());
    assertThat(metadata.getId(), equalTo(original.getId()));
    assertThat(metadata.getTenantId(), equalTo(original.getTenantId()));
    assertThat(metadata.getAccountType(), equalTo(update.getAccountType()));
    assertThat(metadata.getMetadata(), equalTo(update.getMetadata()));
  }

  @Test
  public void testRemoveTenantMetadata() {
    TenantMetadata original = tenantMetadataRepository.save(podamFactory.manufacturePojo(TenantMetadata.class));
    tenantManagement.removeTenantMetadata(original.getTenantId());
    Optional<TenantMetadata> metadata = tenantManagement.getMetadata(original.getTenantId());
    assertTrue(metadata.isEmpty());
  }

}
