package com.rackspace.salus.policy.manage.web.model.validator;

import com.rackspace.salus.policy.manage.web.model.MetadataPolicyCreate;
import com.rackspace.salus.policy.manage.web.model.MetadataPolicyUpdate;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class MetadataPolicyUpdateValidator extends ValueTypeValidator<MetadataPolicyUpdate> {

  @Override
  protected boolean validateValueType(MetadataPolicyUpdate policy) {
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
          // some string that is not a boolean still succeeds with false
          Boolean.parseBoolean(policy.getValue());
          break;
        default:
          //log.warn("Failed to handle policy with valueType={}", policy.getValueType());
      }
    } catch(Exception e) {
      return false;
    }
    return true;
  }

}
