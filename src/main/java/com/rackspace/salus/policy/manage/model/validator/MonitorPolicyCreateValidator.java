package com.rackspace.salus.policy.manage.model.validator;

import com.rackspace.salus.policy.manage.web.model.MonitorPolicyCreate;

public class MonitorPolicyCreateValidator extends PolicyValidator<MonitorPolicyCreate> {
  @Override
  protected Enum getScope(MonitorPolicyCreate policy) {
    return policy.getScope();
  }

  @Override
  protected boolean isSubscopeSet(MonitorPolicyCreate policy) {
    return policy.getSubscope() != null && !policy.getSubscope().isBlank();
  }
}
