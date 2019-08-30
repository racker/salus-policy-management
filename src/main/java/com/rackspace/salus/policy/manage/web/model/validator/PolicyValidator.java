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

import com.rackspace.salus.telemetry.model.PolicyScope;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public abstract class PolicyValidator<T> implements ConstraintValidator<ValidPolicy, T> {
  @Override
  public void initialize(ValidPolicy constraint) { }

  @Override
  public boolean isValid(T value, ConstraintValidatorContext context) {
    return
        // true if scope is global and subset is not set
        (getScope(value).equals(PolicyScope.GLOBAL) && !isSubscopeSet(value)) ||
            // or true if scope is not global and subset is set
            (!getScope(value).equals(PolicyScope.GLOBAL) && isSubscopeSet(value));
  }

  protected abstract PolicyScope getScope(T value);
  protected abstract boolean isSubscopeSet(T value);
}