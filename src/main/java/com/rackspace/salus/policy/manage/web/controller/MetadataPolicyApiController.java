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

import com.rackspace.salus.policy.manage.services.MonitorMetadataPolicyManagement;
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyUpdate;
import com.rackspace.salus.policy.manage.web.model.MonitorMetadataPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.MonitorMetadataPolicyDTO;
import com.rackspace.salus.policy.manage.web.model.ZoneMetadataPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.ZoneMetadataPolicyUpdate;
import com.rackspace.salus.telemetry.model.MonitorType;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PagedContent;
import com.rackspace.salus.telemetry.model.TargetClassName;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  private final MonitorMetadataPolicyManagement monitorMetadataPolicyManagement;

  @Autowired
  public MetadataPolicyApiController(
      MonitorMetadataPolicyManagement monitorMetadataPolicyManagement) {
    this.monitorMetadataPolicyManagement = monitorMetadataPolicyManagement;
  }

  @GetMapping("/admin/policy/metadata/monitor/{uuid}")
  @ApiOperation(value = "Gets specific Metadata Policy by id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Metadata Policy Retrieved")})
  public MonitorMetadataPolicyDTO getById(@PathVariable UUID uuid) throws NotFoundException {
    return new MonitorMetadataPolicyDTO(
        monitorMetadataPolicyManagement.getMetadataPolicy(uuid).orElseThrow(
            () -> new NotFoundException(String.format("No policy found with id %s", uuid))));
  }

  @GetMapping("/admin/policy/metadata/monitor/effective/{tenantId}")
  @ApiOperation(value = "Gets effective Metadata policies by tenant id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policies Retrieved")})
  public List<MonitorMetadataPolicyDTO> getEffectivePoliciesByTenantId(@PathVariable String tenantId) {
    return monitorMetadataPolicyManagement.getEffectiveMetadataPoliciesForTenant(tenantId)
        .stream().map(MonitorMetadataPolicyDTO::new).collect(Collectors.toList());
  }

  @GetMapping("/admin/policy/metadata/monitor/effective/{tenantId}/{className}/{monitorType}")
  @ApiOperation(value = "Gets effective Metadata policies by tenant id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policy values Retrieved")})
  public Map<String, MonitorMetadataPolicyDTO> getPolicyMap(
      @PathVariable String tenantId, @PathVariable TargetClassName className, @PathVariable MonitorType monitorType) {
    return monitorMetadataPolicyManagement.getMetadataPoliciesForTenantAndType(tenantId, className, monitorType)
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            Entry::getKey,
            entry -> new MonitorMetadataPolicyDTO(entry.getValue())
        ));
  }

  @GetMapping("/admin/policy/metadata/monitor")
  @ApiOperation(value = "Gets all monitor metadata policies")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Policies Retrieved")})
  public PagedContent<MonitorMetadataPolicyDTO> getAllMetadataPolicies(Pageable page) {
    return PagedContent.fromPage(monitorMetadataPolicyManagement.getAllMetadataPolicies(page)
        .map(MonitorMetadataPolicyDTO::new));
  }

  @GetMapping("/admin/policy/metadata/zones/{region}")
  @ApiOperation(value = "Gets default monitoring zones for a region")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Zones Retrieved")})
  public List<String> getDefaultMonitoringZones(@PathVariable String region) {
    return monitorMetadataPolicyManagement.getDefaultMonitoringZones(region);
  }

  @PostMapping("/admin/policy/metadata/monitor")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(value = "Creates new monitor metadata Policy")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully Created Metadata Policy")})
  public MonitorMetadataPolicyDTO createMonitorMetadata(@Valid @RequestBody final MonitorMetadataPolicyCreate input)
      throws IllegalArgumentException {
    return new MonitorMetadataPolicyDTO(monitorMetadataPolicyManagement.createMetadataPolicy(input));
  }

  @PostMapping("/admin/policy/metadata/zones")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(value = "Creates new zone metadata Policy")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully Created Metadata Policy")})
  public MonitorMetadataPolicyDTO createZoneMetadata(@Valid @RequestBody final ZoneMetadataPolicyCreate input)
      throws IllegalArgumentException {
    return new MonitorMetadataPolicyDTO(monitorMetadataPolicyManagement.createZonePolicy(
        input.getRegion(), input.getMonitoringZones()));
  }

  @PutMapping("/admin/policy/metadata/monitor/{uuid}")
  @ApiOperation(value = "Updates a monitor metadata Policy")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully Created Metadata Policy")})
  public MonitorMetadataPolicyDTO update(@PathVariable UUID uuid, @Valid @RequestBody final MetadataPolicyUpdate input)
      throws IllegalArgumentException {
    return new MonitorMetadataPolicyDTO(monitorMetadataPolicyManagement.updateMetadataPolicy(uuid, input));
  }

  @PutMapping("/admin/policy/metadata/zones/{region}")
  @ApiOperation(value = "Updates the default monitoring zones for a region")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Zones Retrieved")})
  public MonitorMetadataPolicyDTO updateZoneMetadata(@PathVariable String region, @Valid @RequestBody
      ZoneMetadataPolicyUpdate update) {
    return new MonitorMetadataPolicyDTO(
        monitorMetadataPolicyManagement.updateZonePolicy(region, update.getMonitoringZones()));
  }

  @DeleteMapping("/admin/policy/metadata/monitor/{uuid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiOperation(value = "Deletes specific Metadata Policy")
  @ApiResponses(value = { @ApiResponse(code = 204, message = "Metadata Policy Deleted")})
  public void delete(@PathVariable UUID uuid) {
    monitorMetadataPolicyManagement.removeMetadataPolicy(uuid);
  }

  @DeleteMapping("/admin/policy/metadata/zones/{region}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiOperation(value = "Deletes specific Zone Policy")
  @ApiResponses(value = { @ApiResponse(code = 204, message = "Zone Policy Deleted")})
  public void delete(@PathVariable String region) {
    monitorMetadataPolicyManagement.removeZonePolicy(region);
  }
}
