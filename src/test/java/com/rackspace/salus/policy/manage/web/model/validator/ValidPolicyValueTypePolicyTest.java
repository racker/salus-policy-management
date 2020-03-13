package com.rackspace.salus.policy.manage.web.model.validator;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.rackspace.salus.policy.manage.web.model.MetadataPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyUpdate;
import com.rackspace.salus.telemetry.model.MetadataValueType;
import com.rackspace.salus.telemetry.model.PolicyScope;
import com.rackspace.salus.telemetry.model.TargetClassName;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class ValidPolicyValueTypePolicyTest {
  private LocalValidatorFactoryBean validatorFactoryBean;

  @Before
  public void setup() {
    validatorFactoryBean = new LocalValidatorFactoryBean();
    validatorFactoryBean.afterPropertiesSet();
  }

  @Test
  public void testDurationType() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setTargetClassName(TargetClassName.LocalPlugin)
        .setValueType(MetadataValueType.DURATION)
        .setKey("keyValue")
        .setValue("PT30S");

    final Set<ConstraintViolation<MetadataPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testDurationTypeOnUpdate() {
    MetadataPolicyUpdate policyUpdate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.DURATION)
        .setValue("PT30S");

    final Set<ConstraintViolation<MetadataPolicyUpdate>> errors = validatorFactoryBean.validate(policyUpdate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testDurationTypeFails() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setTargetClassName(TargetClassName.LocalPlugin)
        .setValueType(MetadataValueType.DURATION)
        .setKey("keyValue")
        .setValue("not a duration");

    final Set<ConstraintViolation<MetadataPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(1));
    final ConstraintViolation<MetadataPolicyCreate> violation = errors.iterator().next();
    assertThat(violation.getMessage(), equalTo("Unable to deserialize 'not a duration' as 'DURATION'"));
  }

  @Test
  public void testDurationTypeFailsOnUpdate() {
    MetadataPolicyUpdate policyCreate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.DURATION)
        .setValue("not a duration");

    final Set<ConstraintViolation<MetadataPolicyUpdate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(1));
    final ConstraintViolation<MetadataPolicyUpdate> violation = errors.iterator().next();
    assertThat(violation.getMessage(), equalTo("Unable to deserialize 'not a duration' as 'DURATION'"));
  }

  @Test
  public void testStringType() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setTargetClassName(TargetClassName.LocalPlugin)
        .setValueType(MetadataValueType.STRING)
        .setKey("keyValue")
        .setValue("this is a string");

    final Set<ConstraintViolation<MetadataPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testStringTypeOnUpdate() {
    MetadataPolicyUpdate policyUpdate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.STRING)
        .setValue("this is a string");

    final Set<ConstraintViolation<MetadataPolicyUpdate>> errors = validatorFactoryBean.validate(policyUpdate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testStringListType() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setTargetClassName(TargetClassName.LocalPlugin)
        .setValueType(MetadataValueType.STRING_LIST)
        .setKey("keyValue")
        .setValue("information, other information, more information");

    final Set<ConstraintViolation<MetadataPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(0));
  }


  @Test
  public void testStringListTypeOnUpdate() {
    MetadataPolicyUpdate policyUpdate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.STRING_LIST)
        .setValue("information, other information, more information");

    final Set<ConstraintViolation<MetadataPolicyUpdate>> errors = validatorFactoryBean.validate(policyUpdate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testBooleanType() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setTargetClassName(TargetClassName.LocalPlugin)
        .setValueType(MetadataValueType.BOOL)
        .setKey("keyValue")
        .setValue("TRUE");

    final Set<ConstraintViolation<MetadataPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testBooleanTypeOnUpdate() {
    MetadataPolicyUpdate policyUpdate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.BOOL)
        .setValue("TRUE");

    final Set<ConstraintViolation<MetadataPolicyUpdate>> errors = validatorFactoryBean.validate(policyUpdate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testBooleanTypeAlsoDoesntFail() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setTargetClassName(TargetClassName.LocalPlugin)
        .setValueType(MetadataValueType.BOOL)
        .setKey("keyValue")
        .setValue("not a boolean value");

    final Set<ConstraintViolation<MetadataPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testBooleanTypeAlsoDoesntFailOnUpdate() {
    MetadataPolicyUpdate policyUpdate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.BOOL)
        .setValue("not a boolean value");

    final Set<ConstraintViolation<MetadataPolicyUpdate>> errors = validatorFactoryBean.validate(policyUpdate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testIntegerType() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setTargetClassName(TargetClassName.LocalPlugin)
        .setValueType(MetadataValueType.INT)
        .setKey("keyValue")
        .setValue("123456");

    final Set<ConstraintViolation<MetadataPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testIntegerTypeOnUpdate() {
    MetadataPolicyUpdate policyUpdate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.INT)
        .setValue("123456");

    final Set<ConstraintViolation<MetadataPolicyUpdate>> errors = validatorFactoryBean.validate(policyUpdate);

    assertThat(errors, hasSize(0));
  }

  @Test
  public void testIntegerTypeFails() {
    MetadataPolicyCreate policyCreate = new MetadataPolicyCreate()
        .setScope(PolicyScope.GLOBAL)
        .setTargetClassName(TargetClassName.LocalPlugin)
        .setValueType(MetadataValueType.INT)
        .setKey("keyValue")
        .setValue("not an integer");

    final Set<ConstraintViolation<MetadataPolicyCreate>> errors = validatorFactoryBean.validate(policyCreate);

    assertThat(errors, hasSize(1));
    final ConstraintViolation<MetadataPolicyCreate> violation = errors.iterator().next();
    assertThat(violation.getMessage(), equalTo("Unable to deserialize 'not an integer' as 'INT'"));
  }

  @Test
  public void testIntegerTypeFailsOnUpdate() {
    MetadataPolicyUpdate policyUpdate = new MetadataPolicyUpdate()
        .setValueType(MetadataValueType.INT)
        .setValue("not an integer");

    final Set<ConstraintViolation<MetadataPolicyUpdate>> errors = validatorFactoryBean.validate(policyUpdate);

    assertThat(errors, hasSize(1));
    final ConstraintViolation<MetadataPolicyUpdate> violation = errors.iterator().next();
    assertThat(violation.getMessage(), equalTo("Unable to deserialize 'not an integer' as 'INT'"));
  }
}
