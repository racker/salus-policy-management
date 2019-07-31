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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rackspace.salus.monitor_management.web.client.MonitorApi;
import com.rackspace.salus.monitor_management.web.model.DetailedMonitorOutput;
import com.rackspace.salus.policy.manage.entities.MonitorPolicy;
import com.rackspace.salus.policy.manage.entities.Policy;
import com.rackspace.salus.policy.manage.entities.TenantMetadata;
import com.rackspace.salus.policy.manage.model.Scope;
import com.rackspace.salus.policy.manage.repositories.MonitorPolicyRepository;
import com.rackspace.salus.policy.manage.repositories.PolicyRepository;
import com.rackspace.salus.policy.manage.repositories.TenantMetadataRepository;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.resource_management.web.client.ResourceApi;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.MonitorPolicyEvent;
import com.rackspace.salus.telemetry.messaging.PolicyEvent;
import com.rackspace.salus.telemetry.model.NotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@DataJpaTest(showSql = false)
@Import({PolicyManagement.class, TenantManagement.class})
public class PolicyManagementTest {

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Captor
  ArgumentCaptor<PolicyEvent> policyEventArg;

  @MockBean
  PolicyEventProducer policyEventProducer;

  @MockBean
  ResourceApi resourceApi;

  @MockBean
  MonitorApi monitorApi;

  @Autowired
  PolicyManagement policyManagement;

  @Autowired
  TenantManagement tenantManagement;

  @Autowired
  PolicyRepository policyRepository;

  @Autowired
  MonitorPolicyRepository monitorPolicyRepository;

  @Autowired
  TenantMetadataRepository tenantMetadataRepository;

  private MonitorPolicy defaultMonitorPolicy;

  @Before
  public void setup() {
    Policy policy = new MonitorPolicy()
        .setMonitorId(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setScope(Scope.ACCOUNT_TYPE);

    defaultMonitorPolicy = (MonitorPolicy) policyRepository.save(policy);
  }

  @Test
  public void testGetMonitorPolicy() {
    Optional<Policy> p = policyManagement.getPolicy(defaultMonitorPolicy.getId());

    assertTrue(p.isPresent());
    assertTrue(p.get() instanceof MonitorPolicy);

    MonitorPolicy mp = (MonitorPolicy) p.get();
    assertThat(mp.getId(), notNullValue());
    assertThat(mp.getScope(), isOneOf(Scope.values()));
    assertThat(mp.getScope(), equalTo(defaultMonitorPolicy.getScope()));
    assertThat(mp.getSubscope(), equalTo(defaultMonitorPolicy.getSubscope()));
    assertThat(mp.getName(), equalTo(defaultMonitorPolicy.getName()));
    assertThat(mp.getMonitorId(), equalTo(defaultMonitorPolicy.getMonitorId()));
  }

  @Test
  public void testCreateMonitorPolicy() {
    when(resourceApi.getAllDistinctTenantIds())
        .thenReturn(Collections.singletonList("testCreateMonitorPolicy"));
    when(monitorApi.getPolicyMonitorById(anyString()))
        .thenReturn(podamFactory.manufacturePojo(DetailedMonitorOutput.class));

    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.ACCOUNT_TYPE)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    Policy policy = policyManagement.createMonitorPolicy(policyCreate);
    assertTrue(policy instanceof MonitorPolicy);

    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(policyCreate.getScope()));
    assertThat(policy.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(((MonitorPolicy)policy).getName(), equalTo(policyCreate.getName()));
    assertThat(((MonitorPolicy)policy).getMonitorId(), equalTo(policyCreate.getMonitorId()));

    verify(monitorApi).getPolicyMonitorById(policyCreate.getMonitorId());
    verify(resourceApi).getAllDistinctTenantIds();
    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId("testCreateMonitorPolicy")
    ));

    verifyNoMoreInteractions(monitorApi, resourceApi, policyEventProducer);
  }

  @Test
  public void testCreateMonitorPolicy_multipleTenants() {
    when(resourceApi.getAllDistinctTenantIds())
        .thenReturn(Arrays.asList("tenant1", "tenant2", "tenant3"));
    when(monitorApi.getPolicyMonitorById(anyString()))
        .thenReturn(podamFactory.manufacturePojo(DetailedMonitorOutput.class));

    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.ACCOUNT_TYPE)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    Policy policy = policyManagement.createMonitorPolicy(policyCreate);
    assertTrue(policy instanceof MonitorPolicy);

    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(policyCreate.getScope()));
    assertThat(policy.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(((MonitorPolicy)policy).getName(), equalTo(policyCreate.getName()));
    assertThat(((MonitorPolicy)policy).getMonitorId(), equalTo(policyCreate.getMonitorId()));

    verify(monitorApi).getPolicyMonitorById(policyCreate.getMonitorId());
    verify(resourceApi).getAllDistinctTenantIds();
    verify(policyEventProducer, times(3)).sendPolicyEvent(policyEventArg.capture());
    assertThat(policyEventArg.getAllValues().get(0), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId("tenant1")
    ));
    assertThat(policyEventArg.getAllValues().get(1), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId("tenant2")
    ));
    assertThat(policyEventArg.getAllValues().get(2), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId("tenant3")
    ));

    verifyNoMoreInteractions(monitorApi, resourceApi, policyEventProducer);
  }

  @Test
  public void testCreateMonitorPolicy_duplicatePolicy() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(defaultMonitorPolicy.getScope())
        .setSubscope(defaultMonitorPolicy.getSubscope())
        .setName(defaultMonitorPolicy.getName())
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    assertThatThrownBy(() -> policyManagement.createMonitorPolicy(policyCreate))
      .isInstanceOf(AlreadyExistsException.class)
      .hasMessage(
          String.format("Policy already exists with scope:subscope:name of %s:%s:%s",
              policyCreate.getScope(), policyCreate.getSubscope(), policyCreate.getName())
      );
  }

  @Test
  public void testCreateMonitorPolicy_monitorDoesntExist() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.TENANT)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    when(monitorApi.getPolicyMonitorById(anyString()))
        .thenReturn(null);

    assertThatThrownBy(() -> policyManagement.createMonitorPolicy(policyCreate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            String.format("Invalid monitor id provided: %s",
                policyCreate.getMonitorId())
        );

    verify(monitorApi).getPolicyMonitorById(policyCreate.getMonitorId());
    verifyNoMoreInteractions(monitorApi);
  }

  /**
   * This test verifies that the PolicyEvent contains the id that is actually stored in the db.
   */
  @Test
  public void testPolicyEvent_monitorExistsAfterCreate() {
    when(resourceApi.getAllDistinctTenantIds())
        .thenReturn(Collections.singletonList("testPolicyEvent"));
    when(monitorApi.getPolicyMonitorById(anyString()))
        .thenReturn(podamFactory.manufacturePojo(DetailedMonitorOutput.class));

    // Create a monitor
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.ACCOUNT_TYPE)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    Policy policy = policyManagement.createMonitorPolicy(policyCreate);
    verify(monitorApi).getPolicyMonitorById(policyCreate.getMonitorId());
    verify(resourceApi).getAllDistinctTenantIds();
    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    // Verify the Policy Event looks correct
    assertThat(policyEventArg.getValue(), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId("testPolicyEvent")
    ));

    // Verify the monitor in the PolicyEvent can be found
    Optional<Policy> saved = policyManagement.getPolicy(policyEventArg.getValue().getPolicyId());
    assertTrue(saved.isPresent());

    MonitorPolicy p = (MonitorPolicy) saved.get();
    assertThat(p.getScope(), equalTo(policyCreate.getScope()));
    assertThat(p.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(p.getName(), equalTo(policyCreate.getName()));
    assertThat(p.getMonitorId(), equalTo(policyCreate.getMonitorId()));

    verifyNoMoreInteractions(monitorApi, resourceApi, policyEventProducer);
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
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorId(RandomStringUtils.randomAlphabetic(10))
        .setName("OnlyGlobal")
        .setScope(Scope.GLOBAL)));

    // Create global policy that will be overridden
    policyRepository.save(
        new MonitorPolicy()
            .setMonitorId(RandomStringUtils.randomAlphabetic(10))
            .setName("OverriddenByAccountType")
            .setScope(Scope.GLOBAL)
    );

    // Create AccountType policy that will override global
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorId(RandomStringUtils.randomAlphabetic(10))
        .setName("OverriddenByAccountType")
        .setSubscope(testAccountType)
        .setScope(Scope.ACCOUNT_TYPE)));

    // Create AccountType policy that will be overridden by tenant
    policyRepository.save(
        new MonitorPolicy()
            .setMonitorId(RandomStringUtils.randomAlphabetic(10))
            .setName("OverriddenByTenant")
            .setSubscope(testAccountType)
            .setScope(Scope.ACCOUNT_TYPE)
    );

    // Create AccountType policy that will not be overridden
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorId(RandomStringUtils.randomAlphabetic(10))
        .setName("UniqueAccountPolicy")
        .setSubscope(testAccountType)
        .setScope(Scope.ACCOUNT_TYPE)));

    // Create AccountType policy that is irrelevant to our test tenant.
    policyRepository.save(
        new MonitorPolicy()
            .setMonitorId(RandomStringUtils.randomAlphabetic(10))
            .setName("IrrelevantAccountType")
            .setSubscope("IrrelevantTenantType")
            .setScope(Scope.ACCOUNT_TYPE)
    );

    // Create Tenant policy that will override AccountType
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorId(RandomStringUtils.randomAlphabetic(10))
        .setName("OverriddenByTenant")
        .setSubscope(tenantId)
        .setScope(Scope.TENANT)));

    // Create Tenant policy that will not be overridden
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorId(RandomStringUtils.randomAlphabetic(10))
        .setName("UniqueTenantPolicy")
        .setSubscope(tenantId)
        .setScope(Scope.TENANT)));

    // Create Tenant policy that is irrelevant to our test tenant.
    policyRepository.save(
        new MonitorPolicy()
            .setMonitorId(RandomStringUtils.randomAlphabetic(10))
            .setName("IrrelevantTenant")
            .setSubscope(RandomStringUtils.randomAlphabetic(10))
            .setScope(Scope.TENANT)
    );

    List<Policy> effectivePolicies = policyManagement.getEffectiveMonitorPoliciesForTenant(tenantId);

    assertThat(effectivePolicies, hasSize(5));
    assertThat(effectivePolicies, containsInAnyOrder(expected.toArray()));
  }

  @Test
  public void testRemovePolicy() {
    when(resourceApi.getAllDistinctTenantIds())
        .thenReturn(Collections.singletonList("testRemovePolicy"));

    // Create a policy to remove
    MonitorPolicy saved = (MonitorPolicy) policyRepository.save(new MonitorPolicy()
        .setMonitorId(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setScope(Scope.GLOBAL));

    policyManagement.removePolicy(saved.getId());

    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(saved.getMonitorId())
            .setPolicyId(saved.getId())
            .setTenantId("testRemovePolicy")
    ));

    Optional<Policy> removed = policyManagement.getPolicy(policyEventArg.getValue().getPolicyId());
    assertTrue(removed.isEmpty());
  }

  @Test
  public void testRemovePolicy_doesntExist() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(() -> policyManagement.removePolicy(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            String.format("No policy found with id %s", id)
        );
  }
}