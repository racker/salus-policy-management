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

package com.rackspace.salus.policy.manage.entities;

import com.rackspace.salus.policy.manage.web.model.MonitorPolicyDTO;
import com.rackspace.salus.policy.manage.web.model.PolicyDTO;
import java.time.format.DateTimeFormatter;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Entity
@Table(name = "policy_monitors")
@Data
public class MonitorPolicy extends Policy {

  @NotBlank
  @Column(name="name")
  String name;

  @NotBlank
  @Column(name="monitor_id")
  String monitorId;

  @Override
  public PolicyDTO toDTO() {
    return new MonitorPolicyDTO()
        .setMonitorId(monitorId)
        .setName(name)
        .setSubscope(subscope)
        .setScope(scope)
        .setId(id)
        .setCreatedTimestamp(DateTimeFormatter.ISO_INSTANT.format(createdTimestamp))
        .setUpdatedTimestamp(DateTimeFormatter.ISO_INSTANT.format(updatedTimestamp));
  }
}

