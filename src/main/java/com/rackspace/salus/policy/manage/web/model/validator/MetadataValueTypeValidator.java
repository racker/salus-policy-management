package com.rackspace.salus.policy.manage.web.model.validator;

import com.rackspace.salus.policy.manage.web.model.MetadataPolicyCreate;
import java.text.ParseException;
import java.time.Duration;
import java.util.Arrays;

public class MetadataValueTypeValidator extends ValueTypeValidator<MetadataPolicyCreate> {

  @Override
  protected boolean validateValueType(MetadataPolicyCreate policy) {
    try {
      switch (policy.getValueType()) {
        case STRING:
          policy.getValue();
          break;
        case STRING_LIST:
          Arrays.asList(policy.getValue().split("\\s*,\\s*"));
          break;
        case INT:
          Integer.parseInt(policy.getValue());
          break;
        case DURATION:
          Duration.parse(policy.getValue());
          break;
        case BOOL:
          Boolean.parseBoolean(policy.getValue());
          break;
        default:
          throw new ParseException(String.format("Unable to parse %s as unknown type", policy.getValue()), 0);
      }
    } catch(Exception e) {
      return false;
    }
    return true;
  }
}
