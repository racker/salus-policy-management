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

import com.rackspace.salus.policy.manage.web.model.MetadataPolicyCU;
import com.rackspace.salus.telemetry.entities.MetadataPolicy;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.messaging.MetadataPolicyEvent;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.repositories.MetadataPolicyRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetadataPolicyManagement {

  private final MetadataPolicyRepository metadataPolicyRepository;
  private final PolicyEventProducer policyEventProducer;
  private final PolicyManagement policyManagement;

  public MetadataPolicyManagement(
      MetadataPolicyRepository metadataPolicyRepository,
      PolicyEventProducer policyEventProducer,
      PolicyManagement policyManagement) {
    this.metadataPolicyRepository = metadataPolicyRepository;
    this.policyEventProducer = policyEventProducer;
    this.policyManagement = policyManagement;
  }

  /**
   * Gets the full policy details with the provided id.
   * @param id The id of the policy to retrieve.
   * @return The full policy details.
   */
  public Optional<MetadataPolicy> getMetadataPolicy(UUID id) {
    return metadataPolicyRepository.findById(id);
  }

  /**
   * Returns all the metadata policies configured.
   * @param page The slice of results to be returned.
   * @return A Page of metadata policies.
   */
  public Page<MetadataPolicy> getAllMetadataPolicies(Pageable page) {
    return metadataPolicyRepository.findAll(page);
  }

  /**
   * Gets all the metadata policies relevant to a tenant.
   *
   * Filters the list of all policies down to those that fall into the correct scope/subscope for
   * the provided tenant.
   *
   * @param tenantId The tenantId to retrieve policies for.
   * @return The list of effective metadata policies that should be applied to the tenant's resources.
   */
  public List<MetadataPolicy> getEffectiveMetadataPoliciesForTenant(String tenantId) {
    return
        // Create a stream from all metadata policies
        StreamSupport.stream(metadataPolicyRepository.findAll().spliterator(), false)
            // Filter the stream for only those policies relevant to this tenant
            .filter(policy -> policyManagement.isPolicyApplicable(policy, tenantId))
            // Get one policy for each policy name
            .collect(
                // First group the policies by name
                Collectors.groupingBy(MetadataPolicy::getKey,
                    // then filter each group by only retrieving the one with the highest priority
                    Collectors.maxBy(Comparator.comparingInt(policy -> policy.getScope().getPriority())))
            )
            // Now we have a map of policy name -> (optional) metadata policy
            .values().stream()
            // Filter out any of the optional objects that do not contain anything
            .filter(Optional::isPresent).map(Optional::get)
            // Finally convert to a list of the relevant metadata policies
            .collect(Collectors.toList());
  }

  /**
   * Creates a new policy in the database.
   *
   * @param create The details of the policy to create.
   * @return The full details of the saved policy.
   * @throws AlreadyExistsException if an equivalent policy already exists.
   * @throws IllegalArgumentException if the parameters provided are not valid.
   */
  public MetadataPolicy createMetadataPolicy(@Valid MetadataPolicyCU create)
      throws AlreadyExistsException, IllegalArgumentException {
    if (exists(create)) {
      throw new AlreadyExistsException(String.format("Policy already exists with scope:subscope:key of %s:%s:%s",
          create.getScope(), create.getSubscope(), create.getKey()));
    }
    MetadataPolicy policy = (MetadataPolicy) new MetadataPolicy()
        .setValue(create.getValue())
        .setKey(create.getKey())
        .setValueType(create.getValueType())
        .setMonitorType(create.getMonitorType())
        .setSubscope(create.getSubscope())
        .setScope(create.getScope());

    metadataPolicyRepository.save(policy);
    log.info("Stored new policy {}", policy);
    sendMetadataPolicyEvents(policy);

    return policy;
  }

  /**
   * Removes the metadata policy from the database and sends policy events for each tenant.
   * @param id The id of the policy to remove.
   */
  public void removeMetadataPolicy(UUID id) {
    MetadataPolicy policy = getMetadataPolicy(id).orElseThrow(() ->
        new NotFoundException(
            String.format("No policy found with id %s", id)));

    metadataPolicyRepository.deleteById(id);
    log.info("Removed policy {}", policy);
    sendMetadataPolicyEvents(policy);
  }

  /**
   * Sends metadata policy events for all potentially relevant tenants.
   * @param policy The MetadataPolicy to distribute out to all tenants.
   */
  private void sendMetadataPolicyEvents(MetadataPolicy policy) {
    log.info("Sending metadata policy events for {}", policy);

    List<String> tenantIds = policyManagement.getTenantsForPolicy(policy);

    tenantIds.stream()
        .map(tenantId -> new MetadataPolicyEvent()
            .setPolicyId(policy.getId())
            .setTenantId(tenantId))
        .forEach(policyEventProducer::sendPolicyEvent);
  }

  /**
   * Tests whether an equivalent policy already exists.
   *
   * @param policy The scope, subscope, and name of this policy will be looked up.
   * @return True if a policy already exists for these keys, false otherwise.
   */
  private boolean exists(MetadataPolicyCU policy) {
    return metadataPolicyRepository.existsByScopeAndSubscopeAndKey(
        policy.getScope(), policy.getSubscope(), policy.getKey());
  }
}
