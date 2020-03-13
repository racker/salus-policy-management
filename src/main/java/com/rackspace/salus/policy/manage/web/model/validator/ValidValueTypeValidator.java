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
public @interface ValidValueTypeValidator {
  String message() default "Unable to deserialize type";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
