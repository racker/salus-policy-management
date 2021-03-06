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

package com.rackspace.salus.policy.manage.web.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.rackspace.salus.telemetry.entities.TenantMetadata;
import com.rackspace.salus.common.web.View;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class TenantMetadataDTO {
  UUID id;

  @JsonView(View.Admin.class)
  String tenantId;

  String accountType;

  Map<String,String> metadata;

  String createdTimestamp;
  String updatedTimestamp;

  public TenantMetadataDTO(TenantMetadata tenantMetadata) {
    this.id = tenantMetadata.getId();
    this.tenantId = tenantMetadata.getTenantId();
    this.accountType = tenantMetadata.getAccountType();
    this.metadata = tenantMetadata.getMetadata();
    this.createdTimestamp = DateTimeFormatter.ISO_INSTANT.format(tenantMetadata.getCreatedTimestamp());
    this.updatedTimestamp = DateTimeFormatter.ISO_INSTANT.format(tenantMetadata.getUpdatedTimestamp());
  }
}