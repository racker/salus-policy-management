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

import com.rackspace.salus.policy.manage.entities.TenantMetadata;
import com.rackspace.salus.policy.manage.repositories.TenantMetadataRepository;
import com.rackspace.salus.policy.manage.web.model.TenantMetadataCU;
import com.rackspace.salus.policy.manage.web.model.TenantMetadataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.stereotype.Service;

@Service
public class TenantManagement {

  private final TenantMetadataRepository tenantMetadataRepository;

  @Autowired
  public TenantManagement(
      TenantMetadataRepository tenantMetadataRepository) {
    this.tenantMetadataRepository = tenantMetadataRepository;
  }

  /**
   * Get the account type value for a tenant if it is set.
   *
   * @param tenantId The tenant to lookup.
   * @return The accountType value for the tenant if it exists, otherwise null.
   */
  public String getAccountTypeByTenant(String tenantId) {
    TenantMetadata metadata = tenantMetadataRepository.findByTenantId(tenantId);
    if (metadata == null) {
      return null;
    }
    return metadata.getAccountType();
  }

  /**
   * Create or update the information stored relating to an individual tenant.
   * @param tenantId The tenant to store this data under.
   * @param input The data to alter.
   * @return The full tenant information.
   */
  public TenantMetadataDTO upsertTenantMetadata(String tenantId, TenantMetadataCU input) {
    TenantMetadata metadata = tenantMetadataRepository.findByTenantId(tenantId);

    PropertyMapper map = PropertyMapper.get();
    map.from(tenantId)
        .to(metadata::setTenantId);
    map.from(input.getAccountType())
        .whenNonNull()
        .to(metadata::setAccountType);
    map.from(input.getMetadata())
        .whenNonNull()
        .to(metadata::setMetadata);

    tenantMetadataRepository.save(metadata);

    return metadata.toDTO();
  }
}
