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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rackspace.salus.monitor_management.web.client.MonitorApi;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest(showSql = false)
@Import({PolicyManagement.class, TenantManagement.class})
public class PolicyManagementTest {

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

    verify(resourceApi).getAllDistinctTenantIds();
    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());
    assertThat(policyEventArg.getValue(), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId("testCreateMonitorPolicy")
    ));

    verifyNoMoreInteractions(resourceApi, policyEventProducer);
  }

  @Test
  public void testCreateMonitorPolicy_multipleTenants() {
    when(resourceApi.getAllDistinctTenantIds())
        .thenReturn(Arrays.asList("tenant1", "tenant2", "tenant3"));
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

    verifyNoMoreInteractions(resourceApi, policyEventProducer);
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
  @Ignore
  public void testCreateMonitorPolicy_monitorDoesntExist() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.TENANT)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

//    when(monitorApi.getPolicyMonitorById(anyString()))
//        .thenReturn(false);

    assertThatThrownBy(() -> policyManagement.createMonitorPolicy(policyCreate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            String.format("Invalid monitor id provided: %s",
                policyCreate.getMonitorId())
        );
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
}