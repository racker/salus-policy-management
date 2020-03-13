package com.rackspace.salus.policy.manage.web.model.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy={MetadataValueTypeValidator.class, MetadataPolicyUpdateValidator.class})
public @interface ValidPolicyValueType {
  String message() default "Unable to deserialize '${validatedValue.value}' as '${validatedValue.valueType}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
