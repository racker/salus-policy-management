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

import com.rackspace.salus.common.config.MetricNames;
import com.rackspace.salus.common.config.MetricTagValues;
import com.rackspace.salus.common.config.MetricTags;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyUpdate;
import com.rackspace.salus.policy.manage.web.model.validator.ValidNewPolicy;
import com.rackspace.salus.telemetry.entities.Monitor;
import com.rackspace.salus.telemetry.entities.MonitorPolicy;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.MonitorPolicyEvent;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.repositories.MonitorPolicyRepository;
import com.rackspace.salus.telemetry.repositories.MonitorRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

  MeterRegistry meterRegistry;

  // metrics counters
  private final Counter.Builder monitorPolicySuccess;

  @Autowired
  public MonitorPolicyManagement(
      MonitorRepository monitorRepository,
      MonitorPolicyRepository monitorPolicyRepository,
      PolicyEventProducer policyEventProducer,
      TenantManagement tenantManagement,
      PolicyManagement policyManagement,
      MeterRegistry meterRegistry) {
    this.monitorRepository = monitorRepository;
    this.monitorPolicyRepository = monitorPolicyRepository;
    this.policyEventProducer = policyEventProducer;
    this.policyManagement = policyManagement;

    this.meterRegistry = meterRegistry;
    monitorPolicySuccess = Counter.builder(MetricNames.SERVICE_OPERATION_SUCCEEDED)
        .tag(MetricTags.SERVICE_METRIC_TAG,"MonitorPolicyManagement");
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
    if (!isValidMonitorId(create.getMonitorTemplateId())) {
      throw new IllegalArgumentException(String.format("Invalid monitor template id provided: %s",
          create.getMonitorTemplateId()));
    }
    MonitorPolicy policy = (MonitorPolicy) new MonitorPolicy()
        .setMonitorTemplateId(create.getMonitorTemplateId())
        .setName(create.getName())
        .setSubscope(create.getSubscope())
        .setScope(create.getScope());

    monitorPolicyRepository.save(policy);
    log.info("Stored new policy {}", policy);
    sendMonitorPolicyEvents(policy);
    monitorPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG, MetricTagValues.CREATE_OPERATION,MetricTags.OBJECT_TYPE_METRIC_TAG,"monitorPolicy")
        .register(meterRegistry).increment();
    return policy;
  }

  public MonitorPolicy updateMonitorPolicy(UUID policyId, MonitorPolicyUpdate update) {
    MonitorPolicy policy = getMonitorPolicy(policyId).orElseThrow(() ->
        new NotFoundException(String.format("No policy found for %s", policyId)));

    PolicyScope scope = update.getScope() != null ? update.getScope() : policy.getScope();
    String subscope = update.getSubscope() != null ? update.getSubscope() : policy.getSubscope();
    validateScope(scope, subscope);

    Set<String> originalTenants = new HashSet<>(policyManagement.getTenantsForPolicy(policy));

    policy.setScope(scope);
    policy.setSubscope(subscope);

    // gets the tenants for the updated policy and then creates a union with the original tenants
    Set<String> allRelevantTenants = new HashSet<>(policyManagement.getTenantsForPolicy(policy));
    allRelevantTenants.addAll(originalTenants);

    monitorPolicyRepository.save(policy);
    sendMonitorPolicyEventsForTenants(policy, allRelevantTenants);
    monitorPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG,MetricTagValues.UPDATE_OPERATION,MetricTags.OBJECT_TYPE_METRIC_TAG,"monitorPolicy")
        .register(meterRegistry).increment();
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

  public List<UUID> getEffectiveMonitorTemplateIdsForTenant(String tenantId) {
    return getEffectiveMonitorPoliciesForTenant(tenantId)
        .stream()
        .map(MonitorPolicy::getMonitorTemplateId)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public List<UUID> getEffectiveMonitorPolicyIdsForTenant(String tenantId, boolean includeNullMonitors) {
    return getEffectiveMonitorPoliciesForTenant(tenantId)
        .stream()
        .filter(p -> {
          if (!includeNullMonitors) {
            return p.getMonitorTemplateId() != null;
          }
          return true;
        })
        .map(MonitorPolicy::getId)
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
    monitorPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG,MetricTagValues.REMOVE_OPERATION,MetricTags.OBJECT_TYPE_METRIC_TAG,"monitorPolicy")
        .register(meterRegistry).increment();
  }

  /**
   * Tests whether the policy monitor exists in the MonitorManagement service.
   * @param monitorId The monitor to lookup.
   * @return True if the monitor exists, otherwise false.
   */
  private boolean isValidMonitorId(UUID monitorId) {
    return monitorId == null || monitorRepository.existsByIdAndTenantId(monitorId, Monitor.POLICY_TENANT);
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
    List<String> tenantIds = policyManagement.getTenantsForPolicy(policy);
    sendMonitorPolicyEventsForTenants(policy, tenantIds);
  }

  private void sendMonitorPolicyEventsForTenants(MonitorPolicy policy, Collection<String> tenantIds) {
    log.info("Sending {} monitor policy events for {}", tenantIds.size(), policy);

    if (policy.getMonitorTemplateId() == null) {
      log.debug("Sending opt-out event for policy={}", policy);
    }

    tenantIds.stream()
        .map(tenantId -> new MonitorPolicyEvent()
            .setMonitorId(policy.getMonitorTemplateId())
            .setPolicyId(policy.getId())
            .setTenantId(tenantId))
        .forEach(policyEventProducer::sendPolicyEvent);
  }

  /**
   * Verifies the scope values provided are allowed.
   *
   * A global scope cannot have a subscope (by design global means it affects all tenants).
   * A non-global scope must have a subscope specified to identify the subset of accounts impacted.
   *
   * @param scope The scope of the policy.
   * @param subscope The subscope of the policy.
   * @throws IllegalArgumentException If an invalid combination of the two fields is used.
   */
  private void validateScope(PolicyScope scope, String subscope) throws IllegalArgumentException {
    if ((scope.equals(PolicyScope.GLOBAL) && subscope != null) ||
        (!scope.equals(PolicyScope.GLOBAL) && subscope == null)) {
      throw new IllegalArgumentException(ValidNewPolicy.DEFAULT_MESSAGE);
    }
  }
}