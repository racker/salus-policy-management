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
import com.rackspace.salus.policy.manage.repositories.MonitorPolicyRepository;
import com.rackspace.salus.policy.manage.repositories.PolicyRepository;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.model.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PolicyManagement {

  private final PolicyRepository policyRepository;
  private final MonitorPolicyRepository monitorPolicyRepository;
  private final MonitorApi monitorApi;

  @Autowired
  public PolicyManagement(
      PolicyRepository policyRepository,
      MonitorPolicyRepository monitorPolicyRepository,
      MonitorApi monitorApi) {
    this.policyRepository = policyRepository;
    this.monitorPolicyRepository = monitorPolicyRepository;
    this.monitorApi = monitorApi;
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

    return policy;
  }

  public Optional<Policy> getPolicy(UUID id) {
    return policyRepository.findById(id);
  }

  public void removeMonitorPolicy(UUID id) {
    Policy policy = getPolicy(id).orElseThrow(() ->
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
}
