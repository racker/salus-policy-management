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

package com.rackspace.salus.policy.manage.web.model.validator;

import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import com.rackspace.salus.telemetry.model.PolicyScope;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class MonitorPolicyCreateValidator extends PolicyValidator<MonitorPolicyCreate> {

  @Override
  public boolean isValid(MonitorPolicyCreate policy, ConstraintValidatorContext context) {
    return isValidMonitorId(policy) && super.isValid(policy, context);
  }

  @Override
  protected PolicyScope getScope(MonitorPolicyCreate policy) {
    return policy.getScope();
  }

  @Override
  protected boolean isSubscopeSet(MonitorPolicyCreate policy) {
    return StringUtils.isNotBlank(policy.getSubscope());
  }

  /**
   * Validated the monitorId value is correct.
   * Monitor Id is optional for Tenant policies but required for any other scope.
   *
   * @param policy The policy to validate.
   * @return True if the policy is valid, otherwise false.
   */
  private boolean isValidMonitorId(MonitorPolicyCreate policy) {
    return policy.getScope() == PolicyScope.TENANT || policy.getMonitorTemplateId() != null;
  }
}
