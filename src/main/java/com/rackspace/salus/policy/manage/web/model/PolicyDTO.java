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

import com.rackspace.salus.telemetry.entities.Policy;
import com.rackspace.salus.telemetry.model.PolicyScope;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.Data;

@Data
public abstract class PolicyDTO {
  UUID id;
  PolicyScope scope;
  String subscope;
  String createdTimestamp;
  String updatedTimestamp;

  public PolicyDTO(Policy policy) {
    this.id = policy.getId();
    this.scope = policy.getScope();
    this.subscope = policy.getSubscope();
    this.createdTimestamp = DateTimeFormatter.ISO_INSTANT.format(policy.getCreatedTimestamp());
    this.updatedTimestamp = DateTimeFormatter.ISO_INSTANT.format(policy.getUpdatedTimestamp());
  }
}
