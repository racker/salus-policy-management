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
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyUpdate;
import com.rackspace.salus.policy.manage.web.model.MonitorMetadataPolicyCreate;
import com.rackspace.salus.telemetry.entities.MetadataPolicy;
import com.rackspace.salus.telemetry.entities.MonitorMetadataPolicy;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.MetadataPolicyEvent;
import com.rackspace.salus.telemetry.model.MetadataValueType;
import com.rackspace.salus.telemetry.model.MonitorType;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.model.TargetClassName;
import com.rackspace.salus.telemetry.repositories.MonitorMetadataPolicyRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManager;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MonitorMetadataPolicyManagement {

  private final EntityManager entityManager;
  private final MonitorMetadataPolicyRepository monitorMetadataPolicyRepository;
  private final PolicyEventProducer policyEventProducer;
  private final PolicyManagement policyManagement;

  MeterRegistry meterRegistry;

  CacheManager cacheManager;

  // metrics counters
  private final Counter.Builder createMonitorMetadataPolicySuccess;

  public MonitorMetadataPolicyManagement(
      EntityManager entityManager,
      MonitorMetadataPolicyRepository monitorMetadataPolicyRepository,
      PolicyEventProducer policyEventProducer,
      PolicyManagement policyManagement, MeterRegistry meterRegistry,
      CacheManager cacheManager) {
    this.entityManager = entityManager;
    this.monitorMetadataPolicyRepository = monitorMetadataPolicyRepository;
    this.policyEventProducer = policyEventProducer;
    this.policyManagement = policyManagement;

    this.meterRegistry = meterRegistry;
    createMonitorMetadataPolicySuccess = Counter.builder(MetricNames.SERVICE_OPERATION_SUCCEEDED)
        .tag(MetricTags.SERVICE_METRIC_TAG,"MonitorMetadataPolicyManagement");
    this.cacheManager = cacheManager;
  }

  /**
   * Gets the full policy details with the provided id.
   *
   * @param id The id of the policy to retrieve.
   * @return The full policy details.
   */
  public Optional<MonitorMetadataPolicy> getMetadataPolicy(UUID id) {
    return monitorMetadataPolicyRepository.findById(id);
  }

  /**
   * Returns all the metadata policies configured.
   *
   * @param page The slice of results to be returned.
   * @return A Page of metadata policies.
   */
  public Page<MonitorMetadataPolicy> getAllMetadataPolicies(Pageable page) {
    return monitorMetadataPolicyRepository.findAll(page);
  }

  public Optional<MonitorMetadataPolicy> getZonePolicy(String region) {
    return monitorMetadataPolicyRepository
        .findByScopeAndTargetClassNameAndKey(
            PolicyScope.GLOBAL, TargetClassName.RemotePlugin, MetadataPolicy.ZONE_METADATA_PREFIX + region);
  }

  /**
   * Creates a new policy in the database.
   *
   * @param create The details of the policy to create.
   * @return The full details of the saved policy.
   * @throws AlreadyExistsException if an equivalent policy already exists.
   * @throws IllegalArgumentException if the parameters provided are not valid.
   */
  public MonitorMetadataPolicy createMetadataPolicy(@Valid MonitorMetadataPolicyCreate create)
      throws AlreadyExistsException, IllegalArgumentException {
    if (exists(create)) {
      throw new AlreadyExistsException(String.format("Policy already exists with "
              + "scope:subscope:class:type:key of %s:%s:%s:%s:%s",
          create.getScope(), create.getSubscope(), create.getTargetClassName(),
          create.getMonitorType(), create.getKey()));
    }

    MonitorMetadataPolicy policy;

    switch (create.getTargetClassName()) {
      case Monitor:
      case RemotePlugin:
      case LocalPlugin:
        policy = (MonitorMetadataPolicy) new MonitorMetadataPolicy()
            .setMonitorType(create.getMonitorType())
            .setValue(create.getValue())
            .setKey(create.getKey())
            .setValueType(create.getValueType())
            .setTargetClassName(create.getTargetClassName())
            .setSubscope(create.getSubscope())
            .setScope(create.getScope());
        break;
      default:
        throw new IllegalArgumentException("Invalid target class name found");
    }

    monitorMetadataPolicyRepository.save(policy);
    log.info("Stored new policy {}", policy);
    sendMetadataPolicyEvents(policy);

    createMonitorMetadataPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG, MetricTagValues.CREATE_OPERATION,MetricTags.OBJECT_TYPE_METRIC_TAG,"metadataPolicy")
        .register(meterRegistry).increment();
    return policy;
  }

  /**
   * A helper method to create a monitor metadata entry in the database
   * specifically for monitoring zone regions.
   *
   * @param region The region to create a policy for.
   * @param zones The zones to be used for the provided region.
   * @return The full details of the saved policy.
   * @throws AlreadyExistsException if an equivalent policy already exists.
   * @throws IllegalArgumentException if the parameters provided are not valid.
   */
  public MonitorMetadataPolicy createZonePolicy(String region, List<String> zones)
      throws AlreadyExistsException, IllegalArgumentException {

    MonitorMetadataPolicyCreate convertedCreate = (MonitorMetadataPolicyCreate) new MonitorMetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setSubscope(null)
        .setKey(MetadataPolicy.ZONE_METADATA_PREFIX + region)
        .setTargetClassName(TargetClassName.RemotePlugin)
        .setValue(String.join(",", zones))
        .setValueType(MetadataValueType.STRING_LIST);

    MonitorMetadataPolicy monitorMetadataPolicy = createMetadataPolicy(convertedCreate);
    createMonitorMetadataPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG,MetricTagValues.CREATE_OPERATION,MetricTags.OBJECT_TYPE_METRIC_TAG,"zonePolicy")
        .register(meterRegistry).increment();
    return monitorMetadataPolicy;
  }

  public MonitorMetadataPolicy updateZonePolicy(String region, List<String> zones)
      throws AlreadyExistsException, IllegalArgumentException {
    MonitorMetadataPolicy policy = getZonePolicy(region).orElseThrow(() ->
        new NotFoundException(String.format("No zone policy found for region %s", region)));

    policy.setValue(String.join(",", zones));
    monitorMetadataPolicyRepository.save(policy);
    sendMetadataPolicyEvents(policy);
    createMonitorMetadataPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG,MetricTagValues.UPDATE_OPERATION,MetricTags.OBJECT_TYPE_METRIC_TAG,"zonePolicy")
        .register(meterRegistry).increment();
    return policy;
  }

  public MonitorMetadataPolicy updateMetadataPolicy(UUID id, @Valid MetadataPolicyUpdate update) {
    MonitorMetadataPolicy policy = getMetadataPolicy(id).orElseThrow(() ->
        new NotFoundException(String.format("No policy metadata found for %s", id)));

    log.info("Updating policy metadata={} with new values={}", id, update);

    PropertyMapper map = PropertyMapper.get();
    map.from(update.getValueType())
        .whenNonNull()
        .to(policy::setValueType);
    map.from(update.getValue())
        .whenNonNull()
        .to(policy::setValue);

    policy = monitorMetadataPolicyRepository.save(policy);
    log.info("Policy metadata={} stored with new values={}", id, policy);

    sendMetadataPolicyEvents(policy);
    createMonitorMetadataPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG, MetricTagValues.UPDATE_OPERATION, MetricTags.OBJECT_TYPE_METRIC_TAG,"metadataPolicy")
        .register(meterRegistry).increment();
    return policy;
  }

  /**
   * Removes the metadata policy from the database and sends policy events for each tenant.
   *
   * @param id The id of the policy to remove.
   */
  public void removeMetadataPolicy(UUID id) {
    MonitorMetadataPolicy policy = getMetadataPolicy(id).orElseThrow(() ->
        new NotFoundException(
            String.format("No policy found with id %s", id)));

    monitorMetadataPolicyRepository.deleteById(id);
    log.info("Removed policy {}", policy);
    policyManagement.getTenantsForPolicy(policy)
        .forEach(e -> policyManagement.removePolicyFromCache(e, policy.getTargetClassName(), policy.getMonitorType()));
    sendMetadataPolicyEvents(policy);
    createMonitorMetadataPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG, MetricTagValues.REMOVE_OPERATION, MetricTags.OBJECT_TYPE_METRIC_TAG,"metadataPolicy")
        .register(meterRegistry).increment();
  }

  public void removeZonePolicy(String region) {
    MonitorMetadataPolicy policy = getZonePolicy(region).orElseThrow(() ->
        new NotFoundException(String.format("No zone policy found for region %s", region)));

    log.info("Removed policy {}", policy);
    monitorMetadataPolicyRepository.delete(policy);
    createMonitorMetadataPolicySuccess
        .tags(MetricTags.OPERATION_METRIC_TAG, MetricTagValues.REMOVE_OPERATION,MetricTags.OBJECT_TYPE_METRIC_TAG,"zonePolicy")
        .register(meterRegistry).increment();
  }

  /**
   * Sends metadata policy events for all potentially relevant tenants.
   *
   * @param policy The MetadataPolicy to distribute out to all tenants.
   */
  private void sendMetadataPolicyEvents(MonitorMetadataPolicy policy) {
    log.info("Sending metadata policy events for {}", policy);

    List<String> tenantIds = getTenantsForMetadataPolicy(policy);

    tenantIds.stream()
        .map(tenantId -> new MetadataPolicyEvent()
            .setPolicyId(policy.getId())
            .setTenantId(tenantId))
        .forEach(policyEventProducer::sendPolicyEvent);
  }

  /**
   * Gets all the metadata policies relevant to a tenant.
   *
   * Filters the list of all policies down to those that fall into the correct scope/subscope for
   * the provided tenant.
   *
   * @param tenantId The tenantId to retrieve policies for.
   * @return The list of effective metadata policies that should be applied to the tenant's
   * resources.
   */
  public List<MonitorMetadataPolicy> getEffectiveMetadataPoliciesForTenant(String tenantId) {
    return
        // Create a stream from all metadata policies
        StreamSupport.stream(monitorMetadataPolicyRepository.findAll().spliterator(), false)
            // Filter the stream for only those policies relevant to this tenant
            .filter(policy -> policyManagement.isPolicyApplicable(policy, tenantId))
            // Get one policy for each policy name
            .collect(
                // First group the policies by monitor type and key
                Collectors.groupingBy(policy -> Pair.of(policy.getMonitorType(), policy.getKey()),
                    // then filter each group by only retrieving the one with the highest priority
                    Collectors
                        .maxBy(Comparator.comparingInt(policy -> policy.getScope().getPriority())))
            )
            // Now we have a map of policy name -> (optional) metadata policy
            .values().stream()
            // Filter out any of the optional objects that do not contain anything
            .filter(Optional::isPresent).map(Optional::get)
            // Finally convert to a list of the relevant metadata policies
            .collect(Collectors.toList());
  }

  public Map<String, MonitorMetadataPolicy> getMetadataPoliciesForTenantAndType(String tenantId,
      TargetClassName className, MonitorType monitorType) {
    List<MonitorMetadataPolicy> listOfPolicies = getEffectiveMetadataPoliciesForTenant(tenantId);

    Map<String, MonitorMetadataPolicy> policyValuesMap = new HashMap<>();
    for (MonitorMetadataPolicy policy : listOfPolicies) {
      // Only use policies that are related to the target class name.
      if (policy.getTargetClassName().equals(className)) {
        if (policy.getMonitorType() == null) {
          // Add any generic monitor policy if it does not already have a specific monitor type value set.
          policyValuesMap.putIfAbsent(policy.getKey(), policy);
        } else if (policy.getMonitorType().equals(monitorType)) {
          // Add any policy for the given monitor type.  Override the previously set generic policy if one was already added.
          policyValuesMap.put(policy.getKey(), policy);
        }
      }
    }
    return policyValuesMap;
  }

  public List<String> getDefaultMonitoringZones(String region) {
    if (region == null || region.isBlank()) {
      region = MetadataPolicy.DEFAULT_ZONE;
    }
    log.debug("Getting default zones for region={}", region);

    Optional<MonitorMetadataPolicy> policy = monitorMetadataPolicyRepository
        .findByScopeAndTargetClassNameAndKey(
            PolicyScope.GLOBAL, TargetClassName.RemotePlugin, MetadataPolicy.ZONE_METADATA_PREFIX + region);

    return policy.map(
        monitorMetadataPolicy -> Arrays.asList(monitorMetadataPolicy.getValue().split("\\s*,\\s*")))
        .orElse(Collections.emptyList());
  }

  /**
   * Tests whether an equivalent policy already exists.
   *
   * @param policy The scope, subscope, and name of this policy will be looked up.
   * @return True if a policy already exists for these keys, false otherwise.
   */
  private boolean exists(MonitorMetadataPolicyCreate policy) {
    return monitorMetadataPolicyRepository.existsByScopeAndSubscopeAndTargetClassNameAndMonitorTypeAndKey(
        policy.getScope(), policy.getSubscope(), policy.getTargetClassName(), policy.getMonitorType(), policy.getKey());
  }

  /**
   * Get a list of tenants that the policy may be relevant for.
   *
   * In the end, the consumer of these events will ultimately be responsible for determining
   * if the field is relevant or not.
   *
   * This method potentially returns a superset of those that are relevant.
   *
   * @param policy The policy to send events for.
   * @return A list of tenants that may be relevant to the policy.
   */
  private List<String> getTenantsForMetadataPolicy(MonitorMetadataPolicy policy) {
    List<String> tenantsUsingPolicyKey;
    if (policy.getKey().startsWith(MetadataPolicy.ZONE_METADATA_PREFIX)) {
      tenantsUsingPolicyKey = entityManager
          .createNamedQuery("Monitor.getTenantsUsingZoneMetadata", String.class)
          .getResultList();
    } else if (policy.getTargetClassName().equals(TargetClassName.Monitor)) {
      tenantsUsingPolicyKey = entityManager
          .createNamedQuery("Monitor.getTenantsUsingPolicyMetadataInMonitor", String.class)
          .setParameter("metadataKey", policy.getKey())
          .getResultList();
    } else {
      tenantsUsingPolicyKey = entityManager
          .createNamedQuery("Monitor.getTenantsUsingPolicyMetadataInPlugin", String.class)
          .setParameter("metadataKey", policy.getKey())
          .getResultList();
    }
    if (policy.getScope().equals(PolicyScope.GLOBAL)) {
      return tenantsUsingPolicyKey;
    } else {
      // Rather than sending events for all accounts, we can limit it based on those in this scope.
      List<String> tenantsInPolicyType = policyManagement.getTenantsForPolicy(policy);
      return tenantsUsingPolicyKey
          .stream()
          .distinct()
          .filter(tenantsInPolicyType::contains)
          .collect(Collectors.toList());
    }
  }
}
