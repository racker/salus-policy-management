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
import com.rackspace.salus.policy.manage.services.PolicyManagement;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.PolicyDTO;
import com.rackspace.salus.telemetry.model.View;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

  @PostMapping("/admin/policy/monitors")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(value = "Creates new Monitor for Tenant")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully Created Monitor Policy")})
  @JsonView(View.Admin.class)
  public PolicyDTO create(@Valid @RequestBody final MonitorPolicyCreate input)
      throws IllegalArgumentException {
    return policyManagement.createMonitorPolicy(input).toDTO();
  }
}
