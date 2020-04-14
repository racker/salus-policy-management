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

import com.rackspace.salus.policy.manage.services.TenantManagement;
import com.rackspace.salus.policy.manage.web.model.TenantMetadataCU;
import com.rackspace.salus.policy.manage.web.model.TenantMetadataDTO;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.model.PagedContent;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
public class TenantApiController {

  private TenantManagement tenantManagement;

  @Autowired
  public TenantApiController(
      TenantManagement tenantManagement) {
    this.tenantManagement = tenantManagement;
  }

  @GetMapping("/admin/tenant-metadata/{tenantId}")
  @ApiOperation(value = "Retrieves miscellaneous information stored for a particular tenant")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved tenant metadata")})
  public TenantMetadataDTO getTenantMetadata(@PathVariable String tenantId) {
    return new TenantMetadataDTO(
        tenantManagement.getMetadata(tenantId).orElseThrow(
            () -> new NotFoundException(String.format("No metadata found for tenant %s", tenantId))));
  }

  @GetMapping("/admin/tenant-metadata")
  @ApiOperation(value = "Retrieves miscellaneous information stored for all tenants")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved tenant metadata")})
  public PagedContent<TenantMetadataDTO> getAllTenantMetadata(Pageable page) {
    return PagedContent.fromPage(tenantManagement.getAllMetadata(page)
        .map(TenantMetadataDTO::new));
  }

  @PostMapping("/admin/tenant-metadata")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(value = "Creates information stored for a particular tenant")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully created tenant metadata")})
  public TenantMetadataDTO createTenantMetadata(@RequestBody TenantMetadataCU input) {
    return new TenantMetadataDTO(tenantManagement.upsertTenantMetadata(input.getTenantId(), input));
  }

  @PutMapping("/admin/tenant-metadata/{tenantId}")
  @ApiOperation(value = "Creates or updates miscellaneous information stored for a particular tenant")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully updated tenant metadata")})
  public TenantMetadataDTO upsertTenantMetadata(@PathVariable String tenantId,
      @RequestBody TenantMetadataCU input) {
    return new TenantMetadataDTO(tenantManagement.upsertTenantMetadata(tenantId, input));
  }

  @DeleteMapping("/admin/tenant-metadata/{tenantId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiOperation(value = "Deletes all tenant metadata for an account")
  @ApiResponses(value = { @ApiResponse(code = 204, message = "Successfully removed tenant metadata")})
  public void delete(@PathVariable String tenantId) {
    tenantManagement.removeTenantMetadata(tenantId);
  }
}