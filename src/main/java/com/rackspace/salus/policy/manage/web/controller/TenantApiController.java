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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @PutMapping("/public/{tenantId}/account")
  @ApiOperation(value = "Creates or updates miscellaneous information stored for a particular tenant")
  @ApiResponses(value = { @ApiResponse(code = 201, message = "Successfully Created Monitor Policy")})
  public TenantMetadataDTO upsertTenantMetadata(@PathVariable String tenantId,
      @RequestBody TenantMetadataCU input) {
    return tenantManagement.upsertTenantMetadata(tenantId, input);
  }
}