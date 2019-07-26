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
import com.rackspace.salus.policy.manage.entities.TenantMetadata;
import com.rackspace.salus.policy.manage.model.Scope;
import com.rackspace.salus.policy.manage.model.TenantMetadataKeys;
import com.rackspace.salus.policy.manage.repositories.MonitorPolicyRepository;
import com.rackspace.salus.policy.manage.repositories.PolicyRepository;
import com.rackspace.salus.policy.manage.repositories.TenantMetadataRepository;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.TenantMetadataDTO;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PolicyManagement {

  private final PolicyRepository policyRepository;
  private final MonitorPolicyRepository monitorPolicyRepository;
  private final TenantMetadataRepository tenantMetadataRepository;
  private final MonitorApi monitorApi;
  private final ResourceApi resourceApi;
  private final PolicyEventProducer policyEventProducer;

  @Autowired
  public PolicyManagement(
      PolicyRepository policyRepository,
      MonitorPolicyRepository monitorPolicyRepository,
      TenantMetadataRepository tenantMetadataRepository,
      MonitorApi monitorApi,
      ResourceApi resourceApi,
      PolicyEventProducer policyEventProducer) {
    this.policyRepository = policyRepository;
    this.monitorPolicyRepository = monitorPolicyRepository;
    this.tenantMetadataRepository = tenantMetadataRepository;
    this.monitorApi = monitorApi;
    this.resourceApi = resourceApi;
    this.policyEventProducer = policyEventProducer;
  }

  public Policy createMonitorPolicy(@Valid MonitorPolicyCreate create) {
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
    String accountType = getAccountTypeByTenant(tenantId);

    return new ArrayList<>(
        // Create a stream from all monitor policies
        StreamSupport.stream(monitorPolicyRepository.findAll().spliterator(), false)
            // Filter the stream for only those policies relevant to this tenant
            .filter(policy -> {
              return policy.getScope().equals(Scope.GLOBAL) ||
                  (policy.getScope().equals(Scope.ACCOUNT_TYPE) && policy.getSubscope().equals(accountType)) ||
                  (policy.getScope().equals(Scope.TENANT) && policy.getSubscope().equals(tenantId));
            })
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
            .collect(Collectors.toList()));
  }

  private Iterable<MonitorPolicy> getAllMonitorPolicies() {
    return monitorPolicyRepository.findAll();
  }

  public void removePolicy(UUID id) {
    getPolicy(id).orElseThrow(() ->
        new NotFoundException(
            String.format("No policy found with id %s", id)));

    policyRepository.deleteById(id);
  }

  public boolean isValidMonitorId(String monitorId) {
    //monitorApi.getPolicyMonitorById(monitorId)
    return true; // temporary until monitor management is updated
  }

  public boolean exists(MonitorPolicyCreate policy) {
    return monitorPolicyRepository.existsByScopeAndSubscopeAndName(
        policy.getScope(), policy.getSubscope(), policy.getName());
  }

  /**
   * Sends monitor policy events for all potentially relevant tenants.
   * @param policy The MonitorPolicy to distribute out to all tenants.
   */
  private void sendMonitorPolicyEvents(MonitorPolicy policy) {
    List<String> tenantIds = getAllDistinctTenantIds();
    tenantIds.stream()
        .map(tenantId -> new MonitorPolicyEvent()
            .setMonitorId(policy.getMonitorId())
            .setPolicyId(policy.getId())
            .setTenantId(tenantId))
        .forEach(policyEventProducer::sendPolicyEvent);
  }

  /**
   *
   * @param tenantId
   * @return The accountType value for the tenant if it exists, otherwise null.
   */
  public String getAccountTypeByTenant(String tenantId) {
    TenantMetadata metadata = tenantMetadataRepository.findByTenantId(tenantId);
    if (metadata == null) {
      return null;
    }
    return metadata.getMetadata().get(TenantMetadataKeys.ACCOUNT_TYPE.getKey());
  }

  public TenantMetadataDTO upsertTenantMetadata(String tenantId, String key, String value) {
    TenantMetadata metadata = tenantMetadataRepository.findByTenantId(tenantId);

    if (metadata == null) {
      metadata = new TenantMetadata()
          .setTenantId(tenantId)
          .setMetadata(Collections.singletonMap(key, value));

      tenantMetadataRepository.save(metadata);

      return metadata.toDTO();
    }

    metadata.getMetadata().put(key, value);
    tenantMetadataRepository.save(metadata);// need to verify that this saves the new values.  we might need to create a new map to store in the object vs. updating the existing one.

    return metadata.toDTO();
  }

  /**
   * Queries the ResourceAPI to retrieve all known tenants with at least one resource.
   * @return A list of tenant ids.
   */
  private List<String> getAllDistinctTenantIds() {
    //return resourceApi.getAllDistinctTenantIds();
    return Collections.singletonList("aaaaaa");
  }
}
