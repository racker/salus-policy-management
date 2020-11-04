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

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rackspace.salus.policy.manage.config.DatabaseConfig;
import com.rackspace.salus.telemetry.entities.MetadataPolicy;
import com.rackspace.salus.telemetry.entities.MonitorMetadataPolicy;
import com.rackspace.salus.telemetry.messaging.MetadataPolicyEvent;
import com.rackspace.salus.telemetry.messaging.PolicyEvent;
import com.rackspace.salus.telemetry.model.MetadataValueType;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.model.TargetClassName;
import com.rackspace.salus.telemetry.repositories.MonitorMetadataPolicyRepository;
import com.rackspace.salus.telemetry.repositories.MonitorRepository;
import com.rackspace.salus.telemetry.repositories.PolicyRepository;
import com.rackspace.salus.telemetry.repositories.ResourceRepository;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import com.rackspace.salus.test.EnableTestContainersDatabase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@EnableTestContainersDatabase
@DataJpaTest(showSql = false)
@Import({PolicyManagement.class, MonitorMetadataPolicyManagement.class,
    TenantManagement.class, DatabaseConfig.class, SimpleMeterRegistry.class})
public class MonitorMetadataPolicyManagementTest_Zones {

  @Captor
  ArgumentCaptor<PolicyEvent> policyEventArg;

  @MockBean
  PolicyEventProducer policyEventProducer;

  @Autowired
  PolicyManagement policyManagement;

  @Autowired
  MonitorMetadataPolicyManagement monitorMetadataPolicyManagement;

  @Autowired
  TenantManagement tenantManagement;

  @Autowired
  PolicyRepository policyRepository;

  @Autowired
  MonitorMetadataPolicyRepository monitorMetadataPolicyRepository;

  @Autowired
  TenantMetadataRepository tenantMetadataRepository;

  @Autowired
  ResourceRepository resourceRepository;

  @Autowired
  MonitorRepository monitorRepository;

  @MockBean
  EntityManager entityManager;

  @Mock
  TypedQuery query;

  @Before
  public void setup() {
    List<String> defaultZones = List.of("zone-1", "zone-2");
    MonitorMetadataPolicy policy = (MonitorMetadataPolicy) new MonitorMetadataPolicy()
        .setValue(String.join(",", defaultZones))
        .setKey(MetadataPolicy.ZONE_METADATA_PREFIX + MetadataPolicy.DEFAULT_ZONE)
        .setValueType(MetadataValueType.STRING_LIST)
        .setTargetClassName(TargetClassName.RemotePlugin)
        .setSubscope(null)
        .setScope(PolicyScope.GLOBAL);

    monitorMetadataPolicyRepository.save(policy);
  }

  @Test
  public void testGetDefaultMonitoringZones() {
    List<String> zones = monitorMetadataPolicyManagement.getDefaultMonitoringZones(MetadataPolicy.DEFAULT_ZONE);
    assertThat(zones, equalTo(List.of("zone-1", "zone-2")));
  }

  @Test
  public void testCreateZonePolicy() {
    String tenantId = randomAlphabetic(5);
    String region = randomAlphabetic(5);
    List<String> zones = List.of(randomAlphabetic(5), randomAlphabetic(5));

    mockGetTenantsUsingPolicyKey(List.of(tenantId));

    MonitorMetadataPolicy policy = monitorMetadataPolicyManagement.createZonePolicy(region, zones);
    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(PolicyScope.GLOBAL));
    assertThat(policy.getSubscope(), nullValue());
    assertThat(policy.getMonitorType(), nullValue());
    assertThat(policy.getValueType(), equalTo(MetadataValueType.STRING_LIST));
    assertThat(policy.getKey(), equalTo(MetadataPolicy.ZONE_METADATA_PREFIX + region));
    assertThat(policy.getValue(), equalTo(String.join(",", zones)));

    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MetadataPolicyEvent()
            .setTargetClassName(TargetClassName.RemotePlugin)
            .setPolicyId(policy.getId())
            .setTenantId(tenantId)
    ));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testUpdateZonePolicy() {
    String tenantId = randomAlphabetic(5);
    String region = randomAlphabetic(5);
    List<String> newZones = List.of(randomAlphabetic(5), randomAlphabetic(5));

    MetadataPolicy policy = saveZonePolicy(region);

    mockGetTenantsUsingPolicyKey(List.of(tenantId));

    MonitorMetadataPolicy updatedPolicy = monitorMetadataPolicyManagement.updateZonePolicy(region, newZones);

    assertThat(updatedPolicy.getId(), equalTo(policy.getId()));
    assertThat(updatedPolicy.getKey(), equalTo(MetadataPolicy.ZONE_METADATA_PREFIX + region));
    assertThat(updatedPolicy.getValue(), equalTo(String.join(",", newZones)));

    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());
    assertThat(policyEventArg.getValue(), equalTo(
        new MetadataPolicyEvent()
            .setTargetClassName(TargetClassName.RemotePlugin)
            .setPolicyId(policy.getId())
            .setTenantId(tenantId)
    ));
  }

  @Test
  public void testRemoveZonePolicy() {
    String tenantId = randomAlphabetic(5);
    String region = randomAlphabetic(5);

    // Create a policy to remove
    MetadataPolicy policy = saveZonePolicy(region);

    mockGetTenantsUsingPolicyKey(List.of(tenantId));

    monitorMetadataPolicyManagement.removeZonePolicy(region);
    List<String> afterRemove = monitorMetadataPolicyManagement.getDefaultMonitoringZones(region);
    assertThat(afterRemove, hasSize(0));

    // a zone removal does not trigger any monitors to be rebound
    verifyNoInteractions(policyEventProducer);
  }

  @Test
  public void testRemoveZonePolicy_doesntExist() {
    String region = randomAlphabetic(5);
    assertThatThrownBy(() -> monitorMetadataPolicyManagement.removeZonePolicy(region))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            String.format("No zone policy found for region %s", region)
        );
  }

  private void mockGetTenantsUsingPolicyKey(List<String> tenantIds) {
    when(entityManager.createNamedQuery(anyString(), any())).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(tenantIds);
  }

  private MetadataPolicy saveZonePolicy(String region) {
    List<String> zones = List.of(randomAlphabetic(5), randomAlphabetic(5));
    return (MetadataPolicy) policyRepository.save(new MonitorMetadataPolicy()
        .setValue(String.join(",", zones))
        .setKey(MetadataPolicy.ZONE_METADATA_PREFIX + region)
        .setTargetClassName(TargetClassName.RemotePlugin)
        .setValueType(MetadataValueType.STRING_LIST)
        .setScope(PolicyScope.GLOBAL));
  }
}