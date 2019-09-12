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

import com.rackspace.salus.telemetry.entities.MetadataPolicy;
import com.rackspace.salus.telemetry.repositories.MetadataPolicyRepository;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetadataPolicyManagement {

  private final EntityManager entityManager;
  private final MetadataPolicyRepository metadataPolicyRepository;
  private final PolicyEventProducer policyEventProducer;
  private final PolicyManagement policyManagement;

  public MetadataPolicyManagement(
      EntityManager entityManager,
      MetadataPolicyRepository metadataPolicyRepository,
      PolicyEventProducer policyEventProducer,
      PolicyManagement policyManagement) {
    this.entityManager = entityManager;
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
}
