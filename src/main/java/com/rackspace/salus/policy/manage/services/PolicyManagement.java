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

import com.rackspace.salus.telemetry.entities.Policy;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.repositories.PolicyRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class PolicyManagement {

  private final PolicyRepository policyRepository;
  private final EntityManager entityManager;
  private final TenantManagement tenantManagement;

  public PolicyManagement(
      PolicyRepository policyRepository, EntityManager entityManager,
      TenantManagement tenantManagement) {
    this.policyRepository = policyRepository;
    this.entityManager = entityManager;
    this.tenantManagement = tenantManagement;
  }

  public Optional<Policy> getPolicyById(UUID id) {
    return policyRepository.findById(id);
  }

  List<String> getTenantsForPolicy(Policy policy) {
    List<String> tenantIds;
    switch (policy.getScope()) {
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
    return tenantIds;
  }

  /**
   * Gets a list of all tenants that have at least one resource.
   * @return A list of tenant ids.
   */
  List<String> getAllDistinctTenantIds() {
    return entityManager
        .createNamedQuery("TenantMetadata.getAllDistinctTenants", String.class)
        .getResultList();
  }

  private List<String> getTenantsWithAccountType(String accountType) {
    return entityManager
        .createNamedQuery("TenantMetadata.getByAccountType", String.class)
        .setParameter("accountType", accountType)
        .getResultList();
  }

  /**
   * Determines whether the given policy is relevant to the tenant.
   *
   * @param policy The policy to evaluate.
   * @param tenantId The tenant to evaluate.
   * @return True if the policy is relevant, even if it is currently overridden. False otherwise.
   */
  boolean isPolicyApplicable(Policy policy, String tenantId) {
    String accountType = tenantManagement.getAccountTypeByTenant(tenantId);

    return policy.getScope().equals(PolicyScope.GLOBAL) ||
        (policy.getScope().equals(PolicyScope.ACCOUNT_TYPE) && policy.getSubscope().equals(accountType)) ||
        (policy.getScope().equals(PolicyScope.TENANT) && policy.getSubscope().equals(tenantId));
  }
}
