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

import static com.rackspace.salus.policy.manage.TestUtility.createTenantsOfAccountType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.rackspace.salus.policy.manage.TestUtility;
import com.rackspace.salus.policy.manage.config.DatabaseConfig;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyUpdate;
import com.rackspace.salus.telemetry.entities.Monitor;
import com.rackspace.salus.telemetry.entities.MonitorPolicy;
import com.rackspace.salus.telemetry.entities.Policy;
import com.rackspace.salus.telemetry.entities.TenantMetadata;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.MonitorPolicyEvent;
import com.rackspace.salus.telemetry.messaging.PolicyEvent;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.repositories.MonitorPolicyRepository;
import com.rackspace.salus.telemetry.repositories.MonitorRepository;
import com.rackspace.salus.telemetry.repositories.PolicyRepository;
import com.rackspace.salus.telemetry.repositories.ResourceRepository;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import com.rackspace.salus.test.EnableTestContainersDatabase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
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
@EnableTestContainersDatabase
@DataJpaTest(showSql = false)
@Import({PolicyManagement.class, MonitorPolicyManagement.class,
    TenantManagement.class, DatabaseConfig.class, SimpleMeterRegistry.class})
public class MonitorPolicyManagementTest {

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Captor
  ArgumentCaptor<PolicyEvent> policyEventArg;

  @MockBean
  PolicyEventProducer policyEventProducer;

  @Autowired
  PolicyManagement policyManagement;

  @Autowired
  MonitorPolicyManagement monitorPolicyManagement;

  @Autowired
  TenantManagement tenantManagement;

  @Autowired
  PolicyRepository policyRepository;

  @Autowired
  MonitorPolicyRepository monitorPolicyRepository;

  @Autowired
  TenantMetadataRepository tenantMetadataRepository;

  @Autowired
  ResourceRepository resourceRepository;

  @Autowired
  MonitorRepository monitorRepository;

  @Autowired
  EntityManager entityManager;

  private MonitorPolicy defaultMonitorPolicy;

  @Before
  public void setup() {
    MonitorPolicy policy = (MonitorPolicy) new MonitorPolicy()
        .setMonitorTemplateId(UUID.randomUUID())
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setScope(PolicyScope.ACCOUNT_TYPE);

    defaultMonitorPolicy = monitorPolicyRepository.save(policy);
  }

  @Test
  public void testGetMonitorPolicy() {
    Optional<MonitorPolicy> p = monitorPolicyManagement.getMonitorPolicy(defaultMonitorPolicy.getId());

    assertTrue(p.isPresent());
    MonitorPolicy mp = p.get();
    assertThat(mp.getId(), notNullValue());
    assertThat(mp.getScope(), isOneOf(PolicyScope.values()));
    assertThat(mp.getScope(), equalTo(defaultMonitorPolicy.getScope()));
    assertThat(mp.getSubscope(), equalTo(defaultMonitorPolicy.getSubscope()));
    assertThat(mp.getName(), equalTo(defaultMonitorPolicy.getName()));
    assertThat(mp.getMonitorTemplateId(), equalTo(defaultMonitorPolicy.getMonitorTemplateId()));
  }

  @Test
  public void testCreateMonitorPolicy() {
    // Generate a random tenant and account type for the test
    String accountType = RandomStringUtils.randomAlphabetic(10);
    String tenantId = RandomStringUtils.randomAlphabetic(10);
    Monitor monitor = TestUtility.createPolicyTemplate(monitorRepository);

    // Store a default tenant in the db for that account type
    tenantMetadataRepository.save(new TenantMetadata()
        .setAccountType(accountType)
        .setTenantId(tenantId)
        .setMetadata(Collections.emptyMap()));

    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope(accountType)
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorTemplateId(monitor.getId());

    MonitorPolicy policy = monitorPolicyManagement.createMonitorPolicy(policyCreate);
    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(policyCreate.getScope()));
    assertThat(policy.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(policy.getName(), equalTo(policyCreate.getName()));
    assertThat(policy.getMonitorTemplateId(), equalTo(monitor.getId()));

    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorTemplateId())
            .setPolicyId(policy.getId())
            .setTenantId(tenantId)
    ));

    verifyNoMoreInteractions(policyEventProducer);
  }

  /**
   * This tests creating a tenant scoped policy that opts out of a global policy.
   */
  @Test
  public void testCreateMonitorPolicy_nullMonitor() {
    // Generate a random tenant and account type for the test
    String accountType = RandomStringUtils.randomAlphabetic(10);
    String tenantId = RandomStringUtils.randomAlphabetic(10);

    // Store a default tenant in the db for that account type
    tenantMetadataRepository.save(new TenantMetadata()
        .setAccountType(accountType)
        .setTenantId(tenantId)
        .setMetadata(Collections.emptyMap()));

    MonitorPolicyCreate policyOptOut = new MonitorPolicyCreate()
        .setScope(PolicyScope.TENANT)
        .setSubscope(tenantId)
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorTemplateId(null);

    MonitorPolicy policy = monitorPolicyManagement.createMonitorPolicy(policyOptOut);
    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(policyOptOut.getScope()));
    assertThat(policy.getSubscope(), equalTo(policyOptOut.getSubscope()));
    assertThat(policy.getName(), equalTo(policyOptOut.getName()));
    assertThat(policy.getMonitorTemplateId(), nullValue());

    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(null)
            .setPolicyId(policy.getId())
            .setTenantId(tenantId)
    ));

    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testCreateMonitorPolicy_multipleTenants() {
    List<String> tenantIds = TestUtility.createMultipleTenants(tenantMetadataRepository);
    Monitor monitor = TestUtility.createPolicyTemplate(monitorRepository);

    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorTemplateId(monitor.getId());

    MonitorPolicy policy = monitorPolicyManagement.createMonitorPolicy(policyCreate);
    assertThat(policy.getId(), notNullValue());
    assertThat(policy.getScope(), equalTo(policyCreate.getScope()));
    assertThat(policy.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat((policy).getName(), equalTo(policyCreate.getName()));
    assertThat((policy).getMonitorTemplateId(), equalTo(policyCreate.getMonitorTemplateId()));

    verify(policyEventProducer, times(5)).sendPolicyEvent(policyEventArg.capture());
    assertThat(policyEventArg.getAllValues(), hasSize(5));

    List<MonitorPolicyEvent> expected = tenantIds.stream()
        .map(t -> (MonitorPolicyEvent) new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorTemplateId())
            .setPolicyId(policy.getId())
            .setTenantId(t)).collect(Collectors.toList());

    assertThat(policyEventArg.getAllValues(), containsInAnyOrder(expected.toArray()));
    verifyNoMoreInteractions(policyEventProducer);
  }

  @Test
  public void testCreateMonitorPolicy_duplicatePolicy() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(defaultMonitorPolicy.getScope())
        .setSubscope(defaultMonitorPolicy.getSubscope())
        .setName(defaultMonitorPolicy.getName())
        .setMonitorTemplateId(UUID.randomUUID());

    assertThatThrownBy(() -> monitorPolicyManagement.createMonitorPolicy(policyCreate))
      .isInstanceOf(AlreadyExistsException.class)
      .hasMessage(
          String.format("Policy already exists with scope:subscope:name of %s:%s:%s",
              policyCreate.getScope(), policyCreate.getSubscope(), policyCreate.getName())
      );
  }

  @Test
  public void testCreateMonitorPolicy_monitorDoesntExist() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(PolicyScope.TENANT)
        .setSubscope(RandomStringUtils.randomAlphabetic(10))
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorTemplateId(UUID.randomUUID());

    assertThatThrownBy(() -> monitorPolicyManagement.createMonitorPolicy(policyCreate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            String.format("Invalid monitor template id provided: %s",
                policyCreate.getMonitorTemplateId())
        );
  }

  @Test
  public void testUpdateMonitorPolicy() {
    MonitorPolicy originalPolicy = createAccountTypePolicy();
    String newSubscope = RandomStringUtils.randomAlphabetic(10);

    List<String> tenantsOnNewPolicy = createTenantsOfAccountType(
        tenantMetadataRepository, new Random().nextInt(20) + 5, newSubscope);
    List<String> tenantsOnOriginalPolicy = createTenantsOfAccountType(
        tenantMetadataRepository, new Random().nextInt(20) + 5, originalPolicy.getSubscope());
    createTenantsOfAccountType(
        tenantMetadataRepository, new Random().nextInt(20) + 5, "irrelevantAccounts");

    MonitorPolicyUpdate update = new MonitorPolicyUpdate()
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope(newSubscope);

    MonitorPolicy updatedPolicy = monitorPolicyManagement.updateMonitorPolicy(originalPolicy.getId(), update);

    assertThat(updatedPolicy.getScope(), equalTo(update.getScope()));
    assertThat(updatedPolicy.getSubscope(), equalTo(update.getSubscope()));
    assertThat(updatedPolicy.getMonitorTemplateId(), equalTo(originalPolicy.getMonitorTemplateId()));

    verify(policyEventProducer, times(tenantsOnNewPolicy.size() + tenantsOnOriginalPolicy.size()))
        .sendPolicyEvent(any());
  }

  /**
   * This test verifies that the PolicyEvent contains the id that is actually stored in the db.
   */
  @Test
  public void testPolicyEvent_monitorExistsAfterCreate() {
    Monitor monitor = TestUtility.createPolicyTemplate(monitorRepository);

    // Generate a random tenant and account type for the test
    String accountType = RandomStringUtils.randomAlphabetic(10);
    String tenantId = RandomStringUtils.randomAlphabetic(10);

    // Store a default tenant in the db for that account type
    tenantMetadataRepository.save(new TenantMetadata()
        .setAccountType(accountType)
        .setTenantId(tenantId)
        .setMetadata(Collections.emptyMap()));

    // Create a monitor
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope(accountType)
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorTemplateId(monitor.getId());

    Policy policy = monitorPolicyManagement.createMonitorPolicy(policyCreate);
    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    // Verify the Policy Event looks correct
    assertThat(policyEventArg.getValue(), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(policyCreate.getMonitorTemplateId())
            .setPolicyId(policy.getId())
            .setTenantId(tenantId)
    ));

    // Verify the monitor in the PolicyEvent can be found
    Optional<MonitorPolicy> saved = monitorPolicyManagement
        .getMonitorPolicy(policyEventArg.getValue().getPolicyId());
    assertTrue(saved.isPresent());

    MonitorPolicy p = saved.get();
    assertThat(p.getScope(), equalTo(policyCreate.getScope()));
    assertThat(p.getSubscope(), equalTo(policyCreate.getSubscope()));
    assertThat(p.getName(), equalTo(policyCreate.getName()));
    assertThat(p.getMonitorTemplateId(), equalTo(policyCreate.getMonitorTemplateId()));

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
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorTemplateId(UUID.randomUUID())
        .setName("OnlyGlobal")
        .setScope(PolicyScope.GLOBAL)));

    // Create global policy that will be overridden
    policyRepository.save(
        new MonitorPolicy()
            .setMonitorTemplateId(UUID.randomUUID())
            .setName("OverriddenByAccountType")
            .setScope(PolicyScope.GLOBAL)
    );

    // Create AccountType policy that will override global
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorTemplateId(UUID.randomUUID())
        .setName("OverriddenByAccountType")
        .setSubscope(testAccountType)
        .setScope(PolicyScope.ACCOUNT_TYPE)));

    // Create AccountType policy that will be overridden by tenant
    policyRepository.save(
        new MonitorPolicy()
            .setMonitorTemplateId(UUID.randomUUID())
            .setName("OverriddenByTenant")
            .setSubscope(testAccountType)
            .setScope(PolicyScope.ACCOUNT_TYPE)
    );

    // Create AccountType policy that will not be overridden
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorTemplateId(UUID.randomUUID())
        .setName("UniqueAccountPolicy")
        .setSubscope(testAccountType)
        .setScope(PolicyScope.ACCOUNT_TYPE)));

    // Create AccountType policy that is irrelevant to our test tenant.
    policyRepository.save(
        new MonitorPolicy()
            .setMonitorTemplateId(UUID.randomUUID())
            .setName("IrrelevantAccountType")
            .setSubscope("IrrelevantTenantType")
            .setScope(PolicyScope.ACCOUNT_TYPE)
    );

    // Create Tenant policy that will override AccountType
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorTemplateId(UUID.randomUUID())
        .setName("OverriddenByTenant")
        .setSubscope(tenantId)
        .setScope(PolicyScope.TENANT)));

    // Create Tenant policy that will not be overridden
    expected.add(policyRepository.save(new MonitorPolicy()
        .setMonitorTemplateId(UUID.randomUUID())
        .setName("UniqueTenantPolicy")
        .setSubscope(tenantId)
        .setScope(PolicyScope.TENANT)));

    // Create Tenant policy that is irrelevant to our test tenant.
    policyRepository.save(
        new MonitorPolicy()
            .setMonitorTemplateId(UUID.randomUUID())
            .setName("IrrelevantTenant")
            .setSubscope(RandomStringUtils.randomAlphabetic(10))
            .setScope(PolicyScope.TENANT)
    );

    List<MonitorPolicy> effectivePolicies = monitorPolicyManagement.getEffectiveMonitorPoliciesForTenant(tenantId);

    assertThat(effectivePolicies, hasSize(5));
    assertThat(effectivePolicies, containsInAnyOrder(expected.toArray()));
  }

  @Test
  public void testRemoveMonitorPolicy() {
    String tenantId = TestUtility.createSingleTenant(tenantMetadataRepository);

    // Create a policy to remove
    MonitorPolicy saved = (MonitorPolicy) policyRepository.save(new MonitorPolicy()
        .setMonitorTemplateId(UUID.randomUUID())
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setScope(PolicyScope.GLOBAL));

    monitorPolicyManagement.removeMonitorPolicy(saved.getId());

    verify(policyEventProducer).sendPolicyEvent(policyEventArg.capture());

    assertThat(policyEventArg.getValue(), equalTo(
        new MonitorPolicyEvent()
            .setMonitorId(saved.getMonitorTemplateId())
            .setPolicyId(saved.getId())
            .setTenantId(tenantId)
    ));

    Optional<MonitorPolicy> removed = monitorPolicyManagement.getMonitorPolicy(
        policyEventArg.getValue().getPolicyId());

    assertTrue(removed.isEmpty());
  }

  @Test
  public void testRemoveMonitorPolicy_doesntExist() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(() -> monitorPolicyManagement.removeMonitorPolicy(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            String.format("No policy found with id %s", id)
        );
  }

  @Test
  public void testGetAllDistinctTenants() {
    List<String>expectedIds = TestUtility.createMultipleTenants(tenantMetadataRepository);

    List<String> tenantIds = policyManagement.getAllDistinctTenantIds();

    assertThat(tenantIds, notNullValue());
    assertThat(tenantIds, hasSize(expectedIds.size()));
    assertThat(tenantIds, containsInAnyOrder(expectedIds.toArray()));
  }

  private MonitorPolicy createAccountTypePolicy() {
    return monitorPolicyRepository.save((MonitorPolicy) new MonitorPolicy()
        .setName(RandomStringUtils.randomAlphabetic(5))
        .setMonitorTemplateId(UUID.randomUUID())
        .setScope(PolicyScope.ACCOUNT_TYPE)
        .setSubscope(RandomStringUtils.randomAlphabetic(5)));
  }
}