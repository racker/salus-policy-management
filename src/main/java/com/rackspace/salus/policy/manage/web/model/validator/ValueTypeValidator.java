package com.rackspace.salus.policy.manage.web.model.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public abstract class ValueTypeValidator<T> implements ConstraintValidator<ValidPolicyValueType, T> {

  @Override
  public void initialize(ValidPolicyValueType constraint) {

  }

  @Override
  public boolean isValid(T policy, ConstraintValidatorContext constraintValidatorContext) {

    return validateValueType(policy);
  }

  protected abstract boolean validateValueType(T value);
}
