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

import com.rackspace.salus.telemetry.entities.Monitor;
import com.rackspace.salus.telemetry.messaging.PolicyMonitorUpdateEvent;
import com.rackspace.salus.telemetry.entities.MonitorPolicy;
import com.rackspace.salus.telemetry.repositories.MonitorPolicyRepository;
import com.rackspace.salus.telemetry.repositories.MonitorRepository;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.MonitorPolicyEvent;
import com.rackspace.salus.telemetry.model.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MonitorPolicyManagement {

  private final MonitorRepository monitorRepository;
  private final MonitorPolicyRepository monitorPolicyRepository;
  private final PolicyEventProducer policyEventProducer;
  private final PolicyManagement policyManagement;

  @Autowired
  public MonitorPolicyManagement(
      MonitorRepository monitorRepository,
      MonitorPolicyRepository monitorPolicyRepository,
      PolicyEventProducer policyEventProducer,
      TenantManagement tenantManagement,
      PolicyManagement policyManagement) {
    this.monitorRepository = monitorRepository;
    this.monitorPolicyRepository = monitorPolicyRepository;
    this.policyEventProducer = policyEventProducer;
    this.policyManagement = policyManagement;
  }

  /**
   * Creates a new policy in the database.
   *
   * @param create The details of the policy to create.
   * @return The full details of the saved policy.
   * @throws AlreadyExistsException if an equivalent policy already exists.
   * @throws IllegalArgumentException if the parameters provided are not valid.
   */
  public MonitorPolicy createMonitorPolicy(@Valid MonitorPolicyCreate create)
      throws AlreadyExistsException, IllegalArgumentException {
    if (exists(create)) {
      throw new AlreadyExistsException(String.format("Policy already exists with scope:subscope:name of %s:%s:%s",
          create.getScope(), create.getSubscope(), create.getName()));
    }
    if (!isValidMonitorId(create.getMonitorId())) {
      throw new IllegalArgumentException(String.format("Invalid monitor id provided: %s",
          create.getMonitorId()));
    }
    MonitorPolicy policy = (MonitorPolicy) new MonitorPolicy()
        .setMonitorId(create.getMonitorId())
        .setName(create.getName())
        .setSubscope(create.getSubscope())
        .setScope(create.getScope());

    monitorPolicyRepository.save(policy);
    log.info("Stored new policy {}", policy);
    sendMonitorPolicyEvents(policy);

    return policy;
  }

  /**
   * Gets the full policy details with the provided id.
   * @param id The id of the policy to retrieve.
   * @return The full policy details.
   */
  public Optional<MonitorPolicy> getMonitorPolicy(UUID id) {
    return monitorPolicyRepository.findById(id);
  }

  public List<UUID> getEffectivePolicyMonitorIdsForTenant(String tenantId) {
    return getEffectiveMonitorPoliciesForTenant(tenantId)
        .stream()
        .map(MonitorPolicy::getMonitorId)
        .collect(Collectors.toList());
  }

  /**
   * Returns all the monitor policies configured.
   * @param page The slice of results to be returned.
   * @return A Page of monitor policies.
   */
  public Page<MonitorPolicy> getAllMonitorPolicies(Pageable page) {
    return monitorPolicyRepository.findAll(page);
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
  public List<MonitorPolicy> getEffectiveMonitorPoliciesForTenant(String tenantId) {
    return
        // Create a stream from all monitor policies
        StreamSupport.stream(monitorPolicyRepository.findAll().spliterator(), false)
            // Filter the stream for only those policies relevant to this tenant
            .filter(policy -> policyManagement.isPolicyApplicable(policy, tenantId))
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
   * Removes the monitor policy from the database and sends policy events for each tenant.
   * @param id The id of the policy to remove.
   */
  public void removeMonitorPolicy(UUID id) {
    MonitorPolicy policy = getMonitorPolicy(id).orElseThrow(() ->
        new NotFoundException(
            String.format("No policy found with id %s", id)));

    monitorPolicyRepository.deleteById(id);
    log.info("Removed policy {}", policy);
    sendMonitorPolicyEvents(policy);
  }

  /**
   * Tests whether the policy monitor exists in the MonitorManagement service.
   * @param monitorId The monitor to lookup.
   * @return True if the monitor exists, otherwise false.
   */
  private boolean isValidMonitorId(UUID monitorId) {
    return monitorRepository.existsByIdAndTenantId(monitorId, Monitor.POLICY_TENANT);
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
    log.info("Sending monitor policy events for {}", policy);

    List<String> tenantIds = policyManagement.getTenantsForPolicy(policy);

    tenantIds.stream()
        .map(tenantId -> new MonitorPolicyEvent()
            .setMonitorId(policy.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId(tenantId))
        .forEach(policyEventProducer::sendPolicyEvent);
  }

  void handlePolicyMonitorUpdate(UUID monitorId) {
    Optional<MonitorPolicy> policy = monitorPolicyRepository.findByMonitorId(monitorId);
    if (policy.isEmpty()) {
      log.warn("Ignoring policy monitor update for monitor={}, monitor not used in policy", monitorId);
      return;
    }
    log.info("Sending policy monitor update events for {}", policy.get());

    List<String> tenantIds = policyManagement.getTenantsForPolicy(policy.get());

    tenantIds.stream()
        .map(tenantId -> new PolicyMonitorUpdateEvent()
            .setTenantId(tenantId)
            .setMonitorId(monitorId))
        .forEach(policyEventProducer::sendPolicyMonitorUpdateEvent);
  }
}