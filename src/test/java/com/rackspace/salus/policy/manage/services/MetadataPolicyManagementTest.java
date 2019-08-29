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

package com.rackspace.salus.policy.manage.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rackspace.salus.policy.manage.config.DatabaseConfig;
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyCreate;
import com.rackspace.salus.telemetry.entities.MetadataPolicy;
import com.rackspace.salus.telemetry.entities.Policy;
import com.rackspace.salus.telemetry.entities.Resource;
import com.rackspace.salus.telemetry.entities.TenantMetadata;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.MetadataPolicyEvent;
import com.rackspace.salus.telemetry.messaging.PolicyEvent;
import com.rackspace.salus.telemetry.model.MetadataValueType;
import com.rackspace.salus.telemetry.model.MonitorType;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.repositories.MetadataPolicyRepository;
import com.rackspace.salus.telemetry.repositories.MonitorRepository;
import com.rackspace.salus.telemetry.repositories.PolicyRepository;
import com.rackspace.salus.telemetry.repositories.ResourceRepository;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.RandomStringUtils;
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
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@DataJpaTest(showSql = false)
@Import({PolicyManagement.class, MetadataPolicyManagement.class,
    TenantManagement.class, DatabaseConfig.class})
public class MetadataPolicyManagementTest {

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Captor
  ArgumentCaptor<PolicyEvent> policyEventArg;

  @MockBean
  PolicyEventProducer policyEventProducer;

  @Autowired
  PolicyManagement policyManagement;

  @Autowired
  MetadataPolicyManagement metadataPolicyManagement;

  @Autowired
  TenantManagement tenantManagement;

  @Autowired
  PolicyRepository policyRepository;

  @Autowired
  MetadataPolicyRepository metadataPolicyRepository;

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

  private MetadataPolicy defaultMetadataPolicy;

  @Before
  public void setup() {
    MetadataPolicy policy = (MetadataPolicy) new MetadataPolicy()
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey(RandomStringUtils.randomAlphabetic(10))
        .setValueType(MetadataValueType.STRING)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setScope(PolicyScope.ACCOUNT_TYPE);

    defaultMetadataPolicy = metadataPolicyRepository.save(policy);
  }

  @Test
  public void testGetMetadataPolicy() {
    Optional<MetadataPolicy> p = metadataPolicyManagement.getMetadataPolicy(defaultMetadataPolicy.getId());

    assertTrue(p.isPresent());
    MetadataPolicy mp = p.get();
    assertThat(mp.getId(), notNullValue());
    assertThat(mp.getScope(), isOneOf(PolicyScope.values()));
    assertThat(mp.getScope(), equalTo(defaultMetadataPolicy.getScope()));
    assertThat(mp.getSubscope(), equalTo(defaultMetadataPolicy.getSubscope()));
    assertThat(mp.getMonitorType(), equalTo(defaultMetadataPolicy.getMonitorType()));
    assertThat(mp.getValueType(), equalTo(defaultMetadataPolicy.getValueType()));
    assertThat(mp.getKey(), equalTo(defaultMetadataPolicy.getKey()));
    assertThat(mp.getValue(), equalTo(defaultMetadataPolicy.getValue()));
  }

  @Test
  public void testCreateMetadataPolicy() {
    // Generate a random tenant and account type for the test
    String accountType = RandomStringUtils.randomAlphabetic(10);
    String tenantId = RandomStringUtils.randomAlphabetic(10);

    // Store a default tenant in the db for that account type
    tenantMetadataRepository.save(new TenantMetadata()
        .setAccountType(accountType)
        .setTenantId(tenantId)
        .setMetadata(Collections.emptyMap()));

    mockGetTenantsUsingPolicyKey(List.of(tenantId));

    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope(accountType)
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey(RandomStringUtils.randomAlphabetic(10))
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.ping);

    MetadataPolicy policy = metadataPolicyManagement.createMetadataPolicy(policyCreate);
    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(policyCreate.getScope()));
    assertThat(policy.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(policy.getMonitorType(), equalTo(policyCreate.getMonitorType()));
    assertThat(policy.getValueType(), equalTo(policyCreate.getValueType()));
    assertThat(policy.getKey(), equalTo(policyCreate.getKey()));
    assertThat(policy.getValue(), equalTo(policyCreate.getValue()));

    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MetadataPolicyEvent()
            .setPolicyId(policy.getId())
            .setTenantId(tenantId)
    ));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testCreateMetadataPolicy_multipleTenants() {
    List<String> tenantIds = createMultipleTenants("metadataKey");

    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey(RandomStringUtils.randomAlphabetic(10))
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.x509_cert);

    mockGetTenantsUsingPolicyKey(tenantIds);

    MetadataPolicy policy = metadataPolicyManagement.createMetadataPolicy(policyCreate);
    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(policyCreate.getScope()));
    assertThat(policy.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(policy.getMonitorType(), equalTo(policyCreate.getMonitorType()));
    assertThat(policy.getValueType(), equalTo(policyCreate.getValueType()));
    assertThat(policy.getKey(), equalTo(policyCreate.getKey()));
    assertThat(policy.getValue(), equalTo(policyCreate.getValue()));

    verify(policyEventProducer, times(5)).sendPolicyEvent(policyEventArg.capture());
    assertThat(policyEventArg.getAllValues(), hasSize(5));

    List<MetadataPolicyEvent> expected = tenantIds.stream()
        .map(t -> (MetadataPolicyEvent) new MetadataPolicyEvent()
            .setPolicyId(policy.getId())
            .setTenantId(t)).collect(Collectors.toList());

    assertThat(policyEventArg.getAllValues(), containsInAnyOrder(expected.toArray()));
    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testCreateMetadataPolicy_differentMonitorType() {
    String tenantId = RandomStringUtils.randomAlphabetic(10);

    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(defaultMetadataPolicy.getScope())
        .setSubscope(defaultMetadataPolicy.getSubscope())
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey(defaultMetadataPolicy.getKey())
        .setValueType(MetadataValueType.DURATION)
        .setMonitorType(MonitorType.net_response);

    assertThat(policyCreate.getMonitorType(), not(equalTo(defaultMetadataPolicy.getMonitorType())));

    mockGetTenantsUsingPolicyKey(List.of(tenantId));

    MetadataPolicy policy = metadataPolicyManagement.createMetadataPolicy(policyCreate);
    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(policyCreate.getScope()));
    assertThat(policy.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(policy.getMonitorType(), equalTo(policyCreate.getMonitorType()));
    assertThat(policy.getValueType(), equalTo(policyCreate.getValueType()));
    assertThat(policy.getKey(), equalTo(policyCreate.getKey()));
    assertThat(policy.getValue(), equalTo(policyCreate.getValue()));

    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MetadataPolicyEvent()
            .setPolicyId(policy.getId())
            .setTenantId(tenantId)
    ));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testCreateMetadataPolicy_duplicatePolicy() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(defaultMetadataPolicy.getScope())
        .setSubscope(defaultMetadataPolicy.getSubscope())
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey(defaultMetadataPolicy.getKey())
        .setValueType(MetadataValueType.DURATION)
        .setMonitorType(defaultMetadataPolicy.getMonitorType());

    assertThatThrownBy(() -> metadataPolicyManagement.createMetadataPolicy(policyCreate))
      .isInstanceOf(AlreadyExistsException.class)
      .hasMessage(
          String.format("Policy already exists with scope:subscope:type:key of %s:%s:%s:%s",
              policyCreate.getScope(),
              policyCreate.getSubscope(),
              policyCreate.getMonitorType(),
              policyCreate.getKey())
      );
  }

  /**
   * This test verifies that the PolicyEvent contains the id that is actually stored in the db.
   */
  @Test
  public void testPolicyEvent_policyIdExists() {
    // Generate a random tenant and account type for the test
    String accountType = RandomStringUtils.randomAlphabetic(10);
    String tenantId = RandomStringUtils.randomAlphabetic(10);

    // Store a default tenant in the db for that account type
    tenantMetadataRepository.save(new TenantMetadata()
        .setAccountType(accountType)
        .setTenantId(tenantId)
        .setMetadata(Collections.emptyMap()));
    
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope(accountType)
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey(RandomStringUtils.randomAlphabetic(10))
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.ping);

    mockGetTenantsUsingPolicyKey(List.of(tenantId));

    Policy policy = metadataPolicyManagement.createMetadataPolicy(policyCreate);
    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    // Verify the Policy Event looks correct
    assertThat(policyEventArg.getValue(), equalTo(
        new MetadataPolicyEvent()
            .setPolicyId(policy.getId())
            .setTenantId(tenantId)
    ));

    // Verify the monitor in the PolicyEvent can be found
    Optional<MetadataPolicy> saved = metadataPolicyManagement
        .getMetadataPolicy(policyEventArg.getValue().getPolicyId());
    assertTrue(saved.isPresent());

    MetadataPolicy p = saved.get();
    assertThat(p.getScope(), equalTo(policyCreate.getScope()));
    assertThat(p.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(p.getMonitorType(), equalTo(policyCreate.getMonitorType()));
    assertThat(p.getValueType(), equalTo(policyCreate.getValueType()));
    assertThat(p.getKey(), equalTo(policyCreate.getKey()));
    assertThat(p.getValue(), equalTo(policyCreate.getValue()));

    verifyNoMoreInteractions(policyEventProducer);
  }

  /**
   * This test saves numerous policies to the database while also adding certain ones to
   * the `expected` list.  This list is populated with those policies that we would expect
   * to be effective for the particular test tenant and account type.
   */
  @Test
  public void testGetEffectiveMonitorPoliciesForTenant() {
    String tenantId = RandomStringUtils.randomNumeric(5);
    String testAccountType = "TestAccountType";

    tenantMetadataRepository.save(new TenantMetadata()
        .setTenantId(tenantId)
        .setAccountType(testAccountType)
        .setMetadata(Collections.emptyMap()));

    List<Policy> expected = new ArrayList<>();

    // Create a global policy that will not be overridden
    expected.add(policyRepository.save(new MetadataPolicy()
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey("OnlyGlobal")
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.ping)
        .setScope(PolicyScope.GLOBAL)));

    // Create global policy that will be overridden
    policyRepository.save(
        new MetadataPolicy()
            .setValue(RandomStringUtils.randomAlphabetic(10))
            .setKey("OverriddenByAccountType")
            .setValueType(MetadataValueType.STRING)
            .setMonitorType(MonitorType.ping)
            .setScope(PolicyScope.GLOBAL)
    );

    // Create a global policy with the same key but different monitor type
    // that will not be overridden.
    expected.add(policyRepository.save(new MetadataPolicy()
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey("OnlyGlobal")
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.procstat)
        .setScope(PolicyScope.GLOBAL)));

    // Create AccountType policy that will override global
    expected.add(policyRepository.save(new MetadataPolicy()
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey("OverriddenByAccountType")
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.ping)
        .setSubscope(testAccountType)
        .setScope(PolicyScope.ACCOUNT_TYPE)));

    // Create AccountType policy that will be overridden by tenant
    policyRepository.save(
        new MetadataPolicy()
            .setValue(RandomStringUtils.randomAlphabetic(10))
            .setKey("OverriddenByTenant")
            .setValueType(MetadataValueType.STRING)
            .setMonitorType(MonitorType.ping)
            .setSubscope(testAccountType)
            .setScope(PolicyScope.ACCOUNT_TYPE)
    );

    // Create AccountType policy that will not be overridden
    expected.add(policyRepository.save(new MetadataPolicy()
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey("UniqueAccountPolicy")
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.ping)
        .setSubscope(testAccountType)
        .setScope(PolicyScope.ACCOUNT_TYPE)));

    // Create AccountType policy that is irrelevant to our test tenant.
    policyRepository.save(
        new MetadataPolicy()
            .setValue(RandomStringUtils.randomAlphabetic(10))
            .setKey("IrrelevantAccountType")
            .setValueType(MetadataValueType.STRING)
            .setMonitorType(MonitorType.ping)
            .setSubscope("IrrelevantAccountType")
            .setScope(PolicyScope.ACCOUNT_TYPE)
    );

    // Create Tenant policy that will override AccountType
    expected.add(policyRepository.save(new MetadataPolicy()
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey("OverriddenByTenant")
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.ping)
        .setSubscope(tenantId)
        .setScope(PolicyScope.TENANT)));

    // Create Tenant policy that will not be overridden
    expected.add(policyRepository.save(new MetadataPolicy()
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey("UniqueTenantPolicy")
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.ping)
        .setSubscope(tenantId)
        .setScope(PolicyScope.TENANT)));

    // Create Tenant policy that is irrelevant to our test tenant.
    policyRepository.save(
        new MetadataPolicy()
            .setValue(RandomStringUtils.randomAlphabetic(10))
            .setKey("IrrelevantTenant")
            .setValueType(MetadataValueType.STRING)
            .setMonitorType(MonitorType.ping)
            .setSubscope(RandomStringUtils.randomAlphabetic(10))
            .setScope(PolicyScope.TENANT)
    );

    List<MetadataPolicy> effectivePolicies = metadataPolicyManagement.getEffectiveMetadataPoliciesForTenant(tenantId);

    assertThat(effectivePolicies, hasSize(6));
    assertThat(effectivePolicies, containsInAnyOrder(expected.toArray()));
  }

  @Test
  public void testRemoveMetadataPolicy() {
    String tenantId = createSingleTenant();

    // Create a policy to remove
    MetadataPolicy saved = (MetadataPolicy) policyRepository.save(new MetadataPolicy()
        .setValue(RandomStringUtils.randomAlphabetic(10))
        .setKey(RandomStringUtils.randomAlphabetic(10))
        .setValueType(MetadataValueType.STRING)
        .setMonitorType(MonitorType.disk)
        .setScope(PolicyScope.GLOBAL));

    mockGetTenantsUsingPolicyKey(List.of(tenantId));

    metadataPolicyManagement.removeMetadataPolicy(saved.getId());

    verify(entityManager).createNamedQuery("Monitor.getTenantsUsingTemplateVariable", String.class);
    verify(query).setParameter("variable", saved.getKey());
    verify(query).getResultList();
    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MetadataPolicyEvent()
            .setPolicyId(saved.getId())
            .setTenantId(tenantId)
    ));

    Optional<MetadataPolicy> removed = metadataPolicyManagement.getMetadataPolicy(
        policyEventArg.getValue().getPolicyId());

    assertTrue(removed.isEmpty());

    verifyNoMoreInteractions(entityManager, query, policyEventProducer);
  }

  @Test
  public void testRemoveMetadataPolicy_doesntExist() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(() -> metadataPolicyManagement.removeMetadataPolicy(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            String.format("No policy found with id %s", id)
        );
  }

  private String createSingleTenant() {
    Resource resource = podamFactory.manufacturePojo(Resource.class);
    return resourceRepository.save(resource).getTenantId();
  }

  private List<String> createMultipleTenants(String metadataKey) {
    // update this to create tenants that use the field
    List<Resource> resources = podamFactory.manufacturePojo(ArrayList.class, Resource.class);
    return StreamSupport.stream(resourceRepository.saveAll(resources).spliterator(), false)
        .map(Resource::getTenantId)
        .collect(Collectors.toList());
  }

  private void mockGetTenantsUsingPolicyKey(List<String> tenantIds) {
    when(entityManager.createNamedQuery(anyString(), any())).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(tenantIds);
  }
}