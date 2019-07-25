package com.rackspace.salus.policy.manage.model.validator;

import com.rackspace.salus.policy.manage.entities.Policy;

public class PolicyEntityValidator extends PolicyValidator<Policy> {
  @Override
  protected Enum getScope(Policy policy) {
    return policy.getScope();
  }

  @Override
  protected boolean isSubscopeSet(Policy policy) {
    return policy.getSubscope() != null && !policy.getSubscope().isBlank();
  }
}