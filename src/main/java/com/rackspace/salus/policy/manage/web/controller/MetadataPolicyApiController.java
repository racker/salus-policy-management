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
import com.rackspace.salus.policy.manage.services.MetadataPolicyManagement;
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyDTO;
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyUpdate;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PagedContent;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class MetadataPolicyApiController {

  private MetadataPolicyManagement metadataPolicyManagement;

  @Autowired
  public MetadataPolicyApiController(
      MetadataPolicyManagement metadataPolicyManagement) {
    this.metadataPolicyManagement = metadataPolicyManagement;
  }

  @GetMapping("/admin/policy/metadata/{uuid}")
  @ApiOperation(value = "Gets specific Metadata Policy by id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Metadata Policy Retrieved")})
  @JsonView(View.Admin.class)
  public MetadataPolicyDTO getById(@PathVariable UUID uuid) throws NotFoundException {
    return new MetadataPolicyDTO(
        metadataPolicyManagement.getMetadataPolicy(uuid).orElseThrow(
            () -> new NotFoundException(String.format("No policy found with id %s", uuid))));
  }

  @GetMapping("/admin/policy/metadata/effective/{tenantId}")
  @ApiOperation(value = "Gets effective Metadata policies by tenant id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policies Retrieved")})
  @JsonView(View.Admin.class)
  public List<MetadataPolicyDTO> getEffectivePoliciesByTenantId(@PathVariable String tenantId) {
    return metadataPolicyManagement.getEffectiveMetadataPoliciesForTenant(tenantId)
        .stream().map(MetadataPolicyDTO::new).collect(Collectors.toList());
  }

  @GetMapping("/admin/policy/metadata")
  @ApiOperation(value = "Gets all Metadata policies")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policies Retrieved")})
  @JsonView(View.Admin.class)
  public PagedContent<MetadataPolicyDTO> getAllMetadataPolicies(Pageable page) {
    return PagedContent.fromPage(metadataPolicyManagement.getAllMetadataPolicies(page)
        .map(MetadataPolicyDTO::new));
  }

  @PostMapping("/admin/policy/metadata")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(value = "Creates new Metadata Policy")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully Created Metadata Policy")})
  @JsonView(View.Admin.class)
  public MetadataPolicyDTO create(@Valid @RequestBody final MetadataPolicyCreate input)
      throws IllegalArgumentException {
    return new MetadataPolicyDTO(metadataPolicyManagement.createMetadataPolicy(input));
  }

  @PutMapping("/admin/policy/metadata/{uuid}")
  @ApiOperation(value = "Creates new Metadata Policy")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully Created Metadata Policy")})
  @JsonView(View.Admin.class)
  public MetadataPolicyDTO create(@PathVariable UUID uuid, @Valid @RequestBody final MetadataPolicyUpdate input)
      throws IllegalArgumentException {
    return new MetadataPolicyDTO(metadataPolicyManagement.updateMetadataPolicy(uuid, input));
  }

  @DeleteMapping("/admin/policy/metadata/{uuid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiOperation(value = "Deletes specific Metadata Policy")
  @ApiResponses(value = { @ApiResponse(code = 204, message = "Metadata Policy Deleted")})
  @JsonView(View.Admin.class)
  public void delete(@PathVariable UUID uuid) {
    metadataPolicyManagement.removeMetadataPolicy(uuid);
  }
}
