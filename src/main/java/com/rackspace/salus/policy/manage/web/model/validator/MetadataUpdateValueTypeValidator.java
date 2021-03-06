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
 *
 */

package com.rackspace.salus.policy.manage.web.model.validator;

import com.rackspace.salus.policy.manage.web.model.MetadataPolicyUpdate;
import java.time.Duration;
import org.apache.commons.lang3.BooleanUtils;

public class MetadataUpdateValueTypeValidator extends ValueTypeValidator<MetadataPolicyUpdate> {

  @Override
  protected boolean validateValueType(MetadataPolicyUpdate policy) {
    try {
      switch (policy.getValueType()) {
        case STRING:
          break;
        case STRING_LIST:
          break;
        case INT:
          Integer.parseInt(policy.getValue());
          break;
        case DURATION:
          Duration.parse(policy.getValue());
          break;
        case BOOL:
          BooleanUtils.toBoolean(policy.getValue().toLowerCase(), "true", "false");
          break;
        default:
          throw new IllegalArgumentException(String.format("Unable to parse %s as unknown type", policy.getValue()));
      }
    } catch(Exception e) {
      return false;
    }
    return true;
  }

}
