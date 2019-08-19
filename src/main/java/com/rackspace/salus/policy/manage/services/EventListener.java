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

package com.rackspace.salus.policy.manage.services;

import com.rackspace.salus.common.messaging.KafkaTopicProperties;
import com.rackspace.salus.telemetry.messaging.PolicyMonitorUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@KafkaListener(topics = "#{__listener.topic}")
public class EventListener {

  private final KafkaTopicProperties properties;
  private final PolicyManagement policyManagement;
  private final String topic;

  @Autowired
  public EventListener(KafkaTopicProperties properties, PolicyManagement policyManagement) {
    this.properties = properties;
    this.policyManagement = policyManagement;
    this.topic = this.properties.getPolicies();
  }

  /**
   * This method is used by the __listener.topic magic in the KafkaListener
   * @return The topic to consume
   */
  public String getTopic() {
    return this.topic;
  }

  @KafkaHandler
  public void consumePolicyMonitorUpdateEvents(PolicyMonitorUpdateEvent updateEvent) {
    // Ignore non-null tenant events.  These will be handled by monitor management.
    if (updateEvent.getTenantId() == null) {
      policyManagement.handlePolicyMonitorUpdate(updateEvent.getMonitorId());
    }
  }

  /**
   * The policy topic contains multiple event types.
   * This service does not have to act on them all, so we just ignore them if seen.
   * @param event The event we will be ignoring.
   */
  @KafkaHandler(isDefault = true)
  public void ignoreUnhandledEvents(Object event) {
    log.trace("Ignoring event={} with no handler", event);
  }
}
