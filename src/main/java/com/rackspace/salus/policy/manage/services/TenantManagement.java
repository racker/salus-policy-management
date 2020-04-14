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

import com.rackspace.salus.policy.manage.web.model.TenantMetadataCU;
import com.rackspace.salus.telemetry.entities.TenantMetadata;
import com.rackspace.salus.telemetry.messaging.TenantPolicyChangeEvent;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TenantManagement {

  private final TenantMetadataRepository tenantMetadataRepository;
  private final PolicyEventProducer policyEventProducer;

  @Autowired
  public TenantManagement(
      TenantMetadataRepository tenantMetadataRepository,
      PolicyEventProducer policyEventProducer) {
    this.tenantMetadataRepository = tenantMetadataRepository;
    this.policyEventProducer = policyEventProducer;
  }

  /**
   * Gets all known tenant metadata for a single tenant.
   * @param tenantId The tenant to get the info for.
   * @return The full TenantMetadata object.
   */
  public Optional<TenantMetadata> getMetadata(String tenantId) {
    return tenantMetadataRepository.findByTenantId(tenantId);
  }
  /**
   * Gets all known tenant metadata for a single tenant.
   * @param page The slice of results to be returned.
   * @return A Page of tenant metadata.
   */
  public Page<TenantMetadata> getAllMetadata(Pageable page) {
    return tenantMetadataRepository.findAll(page);
  }


  /**
   * Get the account type value for a tenant if it is set.
   *
   * @param tenantId The tenant to lookup.
   * @return The accountType value for the tenant if it exists, otherwise null.
   */
  public String getAccountTypeByTenant(String tenantId) {
    Optional<TenantMetadata> metadata = getMetadata(tenantId);
    if (metadata.isEmpty()) {
      return null;
    }
    return metadata.get().getAccountType();
  }

  /**
   * Create or update the information stored relating to an individual tenant.
   * @param tenantId The tenant to store this data under.
   * @param input The data to alter.
   * @return The full tenant information.
   */
  public TenantMetadata upsertTenantMetadata(String tenantId, TenantMetadataCU input) {
    Optional<TenantMetadata> metadata = tenantMetadataRepository.findByTenantId(tenantId);

    TenantMetadata updated;
    if (metadata.isEmpty()) {
      log.info("Creating tenant metadata for {}", tenantId);
      updated = new TenantMetadata()
        .setTenantId(tenantId);
    } else {
      log.info("Updating tenant metadata for {}", tenantId);
      updated = metadata.get();
    }

    PropertyMapper map = PropertyMapper.get();
    map.from(input.getAccountType())
        .whenNonNull()
        .to(updated::setAccountType);
    map.from(input.getMetadata())
        .whenNonNull()
        .to(updated::setMetadata);

    tenantMetadataRepository.save(updated);
    sendTenantChangeEvents(tenantId);

    return updated;
  }

  public void removeTenantMetadata(String tenantId) {
    TenantMetadata metadata = getMetadata(tenantId).orElseThrow(() ->
        new NotFoundException(
            String.format("No metadata found for tenant %s", tenantId)));

    tenantMetadataRepository.delete(metadata);
    sendTenantChangeEvents(tenantId);
  }

  private void sendTenantChangeEvents(String tenantId) {
    policyEventProducer.sendTenantChangeEvent(
        new TenantPolicyChangeEvent().setTenantId(tenantId));
  }
}
