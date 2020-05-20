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

package com.rackspace.salus.policy.manage.web.controller;

import com.rackspace.salus.policy.manage.services.MonitorPolicyManagement;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyDTO;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyUpdate;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PagedContent;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class MonitorPolicyApiController {

  private MonitorPolicyManagement monitorPolicyManagement;

  @Autowired
  public MonitorPolicyApiController(
      MonitorPolicyManagement monitorPolicyManagement) {
    this.monitorPolicyManagement = monitorPolicyManagement;
  }

  @GetMapping("/admin/policy/monitors/{uuid}")
  @ApiOperation(value = "Gets specific Monitor Policy by id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Monitor Policy Retrieved")})
  public MonitorPolicyDTO getById(@PathVariable UUID uuid) throws NotFoundException {
    return new MonitorPolicyDTO(
        monitorPolicyManagement.getMonitorPolicy(uuid).orElseThrow(
            () -> new NotFoundException(String.format("No policy found with id %s", uuid))));
  }

  @GetMapping("/admin/policy/monitors/effective/{tenantId}")
  @ApiOperation(value = "Gets effective monitor policies by tenant id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policies Retrieved")})
  public List<MonitorPolicyDTO> getEffectivePoliciesByTenantId(@PathVariable String tenantId) {
    return monitorPolicyManagement.getEffectiveMonitorPoliciesForTenant(tenantId)
        .stream().map(MonitorPolicyDTO::new).collect(Collectors.toList());
  }

  @GetMapping("/admin/policy/monitors/effective/{tenantId}/monitor-ids")
  @ApiOperation(value = "Gets effective policy monitor ids by tenant id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policy Monitor ids retrieved")})
  public List<UUID> getEffectivePolicyMonitorIdsForTenant(@PathVariable String tenantId) {
    return monitorPolicyManagement.getEffectivePolicyMonitorIdsForTenant(tenantId);
  }

  @GetMapping("/admin/policy/monitors/effective/{tenantId}/policy-ids")
  @ApiOperation(value = "Gets effective policy monitor ids by tenant id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Monitor Policy ids retrieved")})
  public List<UUID> getEffectiveMonitorPolicyIdsForTenant(@PathVariable String tenantId,
      @RequestParam(required = false, defaultValue = "true") boolean includeNullMonitors) {
    return monitorPolicyManagement.getEffectiveMonitorPolicyIdsForTenant(tenantId, includeNullMonitors);
  }

  @GetMapping("/admin/policy/monitors")
  @ApiOperation(value = "Gets all monitor policies")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policies Retrieved")})
  public PagedContent<MonitorPolicyDTO> getAllMonitorPolicies(Pageable page) {
    return PagedContent.fromPage(monitorPolicyManagement.getAllMonitorPolicies(page)
        .map(MonitorPolicyDTO::new));
  }

  @PutMapping("/admin/policy/monitors/{uuid}")
  @ApiOperation(value = "Updates the scope of an existing monitor policy")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully updated Monitor Policy")})
  public MonitorPolicyDTO update(@PathVariable UUID uuid, @RequestBody final MonitorPolicyUpdate input)
      throws IllegalArgumentException {
    return new MonitorPolicyDTO(monitorPolicyManagement.updateMonitorPolicy(uuid, input));
  }

  @PostMapping("/admin/policy/monitors")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(value = "Creates new Monitor Policy")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully Created Monitor Policy")})
  public MonitorPolicyDTO create(@Valid @RequestBody final MonitorPolicyCreate input)
      throws IllegalArgumentException {
    return new MonitorPolicyDTO(monitorPolicyManagement.createMonitorPolicy(input));
  }

  @DeleteMapping("/admin/policy/monitors/{uuid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiOperation(value = "Deletes specific Monitor Policy")
  @ApiResponses(value = { @ApiResponse(code = 204, message = "Monitor Policy Deleted")})
  public void delete(@PathVariable UUID uuid) {
    monitorPolicyManagement.removeMonitorPolicy(uuid);
  }

  @PostMapping("/admin/policy/monitors/opt-out")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(value = "Opt-out of an existing Monitor Policy")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully opted out of Monitor Policy")})
  public MonitorPolicyDTO optOut(@Valid @RequestBody final MonitorPolicyCreate input)
      throws IllegalArgumentException {
    if (input.getMonitorId() != null) {
      throw new IllegalArgumentException("monitorId cannot be set when opting out of policy");
    }
    return new MonitorPolicyDTO(monitorPolicyManagement.createMonitorPolicy(input));
  }
}
