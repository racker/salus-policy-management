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

import com.rackspace.salus.monitor_management.web.client.MonitorApi;
import com.rackspace.salus.policy.manage.entities.MonitorPolicy;
import com.rackspace.salus.policy.manage.entities.Policy;
import com.rackspace.salus.policy.manage.model.Scope;
import com.rackspace.salus.policy.manage.repositories.MonitorPolicyRepository;
import com.rackspace.salus.policy.manage.repositories.PolicyRepository;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.resource_management.web.client.ResourceApi;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.MonitorPolicyEvent;
import com.rackspace.salus.telemetry.model.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManager;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PolicyManagement {

  private final PolicyRepository policyRepository;
  private final MonitorPolicyRepository monitorPolicyRepository;
  private final MonitorApi monitorApi;
  private final ResourceApi resourceApi;
  private final PolicyEventProducer policyEventProducer;
  private final TenantManagement tenantManagement;
  private final EntityManager entityManager;

  @Autowired
  public PolicyManagement(
      PolicyRepository policyRepository,
      MonitorPolicyRepository monitorPolicyRepository,
      MonitorApi monitorApi,
      ResourceApi resourceApi,
      PolicyEventProducer policyEventProducer,
      TenantManagement tenantManagement, EntityManager entityManager) {
    this.policyRepository = policyRepository;
    this.monitorPolicyRepository = monitorPolicyRepository;
    this.monitorApi = monitorApi;
    this.resourceApi = resourceApi;
    this.policyEventProducer = policyEventProducer;
    this.tenantManagement = tenantManagement;
    this.entityManager = entityManager;
  }

  /**
   * Creates a new policy in the database.
   *
   * @param create The details of the policy to create.
   * @return The full details of the saved policy.
   * @throws AlreadyExistsException if an equivalent policy already exists.
   * @throws IllegalArgumentException if the parameters provided are not valid.
   */
  public Policy createMonitorPolicy(@Valid MonitorPolicyCreate create)
      throws AlreadyExistsException, IllegalArgumentException {
    if (exists(create)) {
      throw new AlreadyExistsException(String.format("Policy already exists with scope:subscope:name of %s:%s:%s",
          create.getScope(), create.getSubscope(), create.getName()));
    }
    if (!isValidMonitorId(create.getMonitorId())) {
      throw new IllegalArgumentException(String.format("Invalid monitor id provided: %s",
          create.getMonitorId()));
    }
    Policy policy = new MonitorPolicy()
        .setMonitorId(create.getMonitorId())
        .setName(create.getName())
        .setSubscope(create.getSubscope())
        .setScope(create.getScope());

    policyRepository.save(policy);
    sendMonitorPolicyEvents((MonitorPolicy) policy);

    return policy;
  }

  /**
   * Gets the full policy details with the provided id.
   * @param id The id of the policy to retrieve.
   * @return The full policy details.
   */
  public Optional<Policy> getPolicy(UUID id) {
    return policyRepository.findById(id);
  }

  /**
   * Gets all the monitor policies relevant to a tenant.
   *
   * Filters the list of all policies down to those that fall into the correct scope/subscope for
   * the provided tenant.
   *
   * @param tenantId The tenantId to retrieve policies for.
   * @return The list of effective monitor policies that should be applied to the tenant's resources.
   */
  public List<Policy> getEffectiveMonitorPoliciesForTenant(String tenantId) {
    return
        // Create a stream from all monitor policies
        StreamSupport.stream(monitorPolicyRepository.findAll().spliterator(), false)
            // Filter the stream for only those policies relevant to this tenant
            .filter(policy -> isPolicyApplicable(policy, tenantId))
            // Get one policy for each policy name
            .collect(
                // First group the policies by name
                Collectors.groupingBy(MonitorPolicy::getName,
                    // then filter each group by only retrieving the one with the highest priority
                    Collectors.maxBy(Comparator.comparingInt(policy -> policy.getScope().getPriority())))
            )
            // Now we have a map of policy name -> (optional) monitor policy
            .values().stream()
            // Filter out any of the optional objects that do not contain anything
            .filter(Optional::isPresent).map(Optional::get)
            // Finally convert to a list of the relevant monitor policies
            .collect(Collectors.toList());
  }

  /**
   * Determines whether the given policy is relevant to the tenant.
   *
   * @param policy The policy to evaluate.
   * @param tenantId The tenant to evaluate.
   * @return True if the policy is relevant, even if it is currently overridden. False otherwise.
   */
  private boolean isPolicyApplicable(Policy policy, String tenantId) {
    String accountType = tenantManagement.getAccountTypeByTenant(tenantId);

    return policy.getScope().equals(Scope.GLOBAL) ||
        (policy.getScope().equals(Scope.ACCOUNT_TYPE) && policy.getSubscope().equals(accountType)) ||
        (policy.getScope().equals(Scope.TENANT) && policy.getSubscope().equals(tenantId));
  }

  /**
   * Removes the policy from the database and sends policy events for each tenant.
   * @param id The id of the policy to remove.
   */
  public void removePolicy(UUID id) {
    Policy policy = getPolicy(id).orElseThrow(() ->
        new NotFoundException(
            String.format("No policy found with id %s", id)));

    policyRepository.deleteById(id);
    sendMonitorPolicyEvents((MonitorPolicy) policy);
  }

  /**
   * Tests whether the monitor exists in the MonitorManagement service.
   * @param monitorId The monitor to lookup.
   * @return True if the monitor exists, otherwise false.
   */
  private boolean isValidMonitorId(String monitorId) {
    return monitorApi.getPolicyMonitorById(monitorId) != null;
  }

  /**
   * Tests whether an equivalent policy already exists.
   *
   * @param policy The scope, subscope, and name of this policy will be looked up.
   * @return True if a policy already exists for these keys, false otherwise.
   */
  private boolean exists(MonitorPolicyCreate policy) {
    return monitorPolicyRepository.existsByScopeAndSubscopeAndName(
        policy.getScope(), policy.getSubscope(), policy.getName());
  }

  /**
   * Sends monitor policy events for all potentially relevant tenants.
   * @param policy The MonitorPolicy to distribute out to all tenants.
   */
  private void sendMonitorPolicyEvents(MonitorPolicy policy) {
    List<String> tenantIds;

    switch(policy.getScope()) {
      case GLOBAL:
        tenantIds = getAllDistinctTenantIds();
        break;
      case ACCOUNT_TYPE:
        tenantIds = getTenantsWithAccountType(policy.getSubscope());
        break;
      case TENANT:
        tenantIds = Collections.singletonList(policy.getSubscope());
        break;
      default:
        tenantIds = Collections.emptyList();
        break;
    }
    tenantIds.stream()
        .map(tenantId -> new MonitorPolicyEvent()
            .setMonitorId(policy.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId(tenantId))
        .forEach(policyEventProducer::sendPolicyEvent);
  }

  /**
   * Queries the ResourceAPI to retrieve all known tenants with at least one resource.
   * @return A list of tenant ids.
   */
  private List<String> getAllDistinctTenantIds() {
    return resourceApi.getAllDistinctTenantIds();
  }

  private List<String> getTenantsWithAccountType(String accountType) {
    return entityManager
        .createNamedQuery("Tenant.getByAccountType", String.class)
        .setParameter("accountType", accountType)
        .getResultList();
  }
}