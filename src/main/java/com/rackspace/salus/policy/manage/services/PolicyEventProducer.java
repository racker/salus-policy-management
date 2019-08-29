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

import static com.rackspace.salus.common.messaging.KafkaMessageKeyBuilder.buildMessageKey;

import com.rackspace.salus.common.messaging.KafkaTopicProperties;
import com.rackspace.salus.telemetry.messaging.PolicyEvent;
import com.rackspace.salus.telemetry.messaging.PolicyMonitorUpdateEvent;
import com.rackspace.salus.telemetry.messaging.TenantPolicyChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PolicyEventProducer {

  private final KafkaTemplate<String,Object> kafkaTemplate;
  private final KafkaTopicProperties properties;

  @Autowired
  public PolicyEventProducer(KafkaTemplate<String,Object> kafkaTemplate, KafkaTopicProperties properties) {
    this.kafkaTemplate = kafkaTemplate;
    this.properties= properties;
  }

  void sendPolicyEvent(PolicyEvent event) {
    final String topic = properties.getPolicies();

    log.debug("Sending policyEvent={} on topic={}", event, topic);
    kafkaTemplate.send(topic, buildMessageKey(event), event);
  }

  void sendPolicyMonitorUpdateEvent(PolicyMonitorUpdateEvent event) {
    final String topic = properties.getPolicies();

    log.debug("Sending policyMonitorUpdateEvent={} on topic={}", event, topic);
    kafkaTemplate.send(topic, buildMessageKey(event), event);
  }

  void sendTenantChangeEvent(TenantPolicyChangeEvent event) {
    final String topic = properties.getPolicies();

    log.debug("Sending tenantChangeEvent={} on topic={}", event, topic);
    kafkaTemplate.send(topic, buildMessageKey(event), event);
  }
}