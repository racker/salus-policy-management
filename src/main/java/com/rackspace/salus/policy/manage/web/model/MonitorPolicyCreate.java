/*
 * Copyright 2020 Rackspace US, Inc.
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

import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.policy.manage.web.model.validator.ValidNewPolicy;
import java.io.Serializable;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * This Object is used for handling the creation of Monitor Policies.
 */
@Data
@ValidNewPolicy
public class MonitorPolicyCreate implements Serializable {
  @NotNull
  PolicyScope scope;

  String subscope;

  @NotBlank
  String name;

  UUID monitorId;
}
