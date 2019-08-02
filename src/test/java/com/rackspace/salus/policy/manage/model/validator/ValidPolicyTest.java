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

package com.rackspace.salus.policy.manage.model.validator;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.rackspace.salus.policy.manage.model.Scope;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class ValidPolicyTest {

  private LocalValidatorFactoryBean validatorFactoryBean;

  @Before
  public void setup() {
    validatorFactoryBean = new LocalValidatorFactoryBean();
    validatorFactoryBean.afterPropertiesSet();
  }

  @Test
  public void testValidGlobal() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.GLOBAL)
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    final Set<ConstraintViolation<MonitorPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testInvalidGlobal() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.GLOBAL)
        .setSubscope("Subscope is not allowed for global scoped policies")
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    final Set<ConstraintViolation<MonitorPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(1));
    final ConstraintViolation<MonitorPolicyCreate> violation = errors.iterator().next();
    assertThat(violation.getMessage(),
        equalTo("subscope must be set for any non-global policy but not for global policies"));
  }

  @Test
  public void testValidAccountType() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.ACCOUNT_TYPE)
        .setSubscope("Subscope is required")
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    final Set<ConstraintViolation<MonitorPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testInvalidAccountType() {
    MonitorPolicyCreate policyCreate = new MonitorPolicyCreate()
        .setScope(Scope.ACCOUNT_TYPE)
        .setSubscope("")
        .setName(RandomStringUtils.randomAlphabetic(10))
        .setMonitorId(RandomStringUtils.randomAlphabetic(10));

    final Set<ConstraintViolation<MonitorPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(1));
    final ConstraintViolation<MonitorPolicyCreate> violation = errors.iterator().next();
    assertThat(violation.getMessage(),
        equalTo("subscope must be set for any non-global policy but not for global policies"));
  }

}
