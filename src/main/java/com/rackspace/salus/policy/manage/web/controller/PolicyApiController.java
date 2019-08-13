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

import com.fasterxml.jackson.annotation.JsonView;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyDTO;
import com.rackspace.salus.policy.manage.services.PolicyManagement;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.PolicyDTO;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.View;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class PolicyApiController {

  private PolicyManagement policyManagement;

  @Autowired
  public PolicyApiController(
      PolicyManagement policyManagement) {
    this.policyManagement = policyManagement;
  }

  @GetMapping("/admin/policy/monitors/{uuid}")
  @ApiOperation(value = "Gets specific Monitor Policy by id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Monitor Policy Retrieved")})
  @JsonView(View.Admin.class)
  public MonitorPolicyDTO getById(@PathVariable UUID uuid) throws NotFoundException {
    return new MonitorPolicyDTO(
        policyManagement.getMonitorPolicy(uuid).orElseThrow(
            () -> new NotFoundException(String.format("No policy found with id %s", uuid))));
  }

  @GetMapping("/admin/policy/monitors/effective/{tenantId}")
  @ApiOperation(value = "Gets effective monitor policies by tenant id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policies Retrieved")})
  @JsonView(View.Admin.class)
  public List<PolicyDTO> getEffectivePoliciesByTenantId(@PathVariable String tenantId) {
    return policyManagement.getEffectiveMonitorPoliciesForTenant(tenantId)
        .stream().map(MonitorPolicyDTO::new).collect(Collectors.toList());
  }
  @GetMapping("/admin/policy/monitors/effective/{tenantId}/ids")
  @ApiOperation(value = "Gets effective policy monitor ids by tenant id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Monitor policy ids retrieved")})
  @JsonView(View.Admin.class)
  public List<UUID> getEffectivePolicyMonitorIdsForTenant(@PathVariable String tenantId) {
    return policyManagement.getEffectivePolicyMonitorIdsForTenant(tenantId);
  }

  @PostMapping("/admin/policy/monitors")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(value = "Creates new Monitor for Tenant")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully Created Monitor Policy")})
  @JsonView(View.Admin.class)
  public MonitorPolicyDTO create(@Valid @RequestBody final MonitorPolicyCreate input)
      throws IllegalArgumentException {
    return new MonitorPolicyDTO(policyManagement.createMonitorPolicy(input));
  }

  @DeleteMapping("/admin/policy/monitors/{uuid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiOperation(value = "Deletes specific Monitor Policy")
  @ApiResponses(value = { @ApiResponse(code = 204, message = "Monitor Policy Deleted")})
  @JsonView(View.Admin.class)
  public void delete(@PathVariable UUID uuid) {
    policyManagement.removeMonitorPolicy(uuid);
  }
}
