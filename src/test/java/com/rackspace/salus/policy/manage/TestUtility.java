/*
 * Copyright 2020 Rackspace US, Inc.
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

package com.rackspace.salus.policy.manage;

import com.rackspace.salus.telemetry.entities.Monitor;
import com.rackspace.salus.telemetry.entities.Resource;
import com.rackspace.salus.telemetry.repositories.MonitorRepository;
import com.rackspace.salus.telemetry.repositories.ResourceRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

public class TestUtility {

  private static PodamFactory podamFactory = new PodamFactoryImpl();

  public static Monitor createPolicyMonitor(MonitorRepository monitorRepository) {
    Monitor monitor = podamFactory.manufacturePojo(Monitor.class);
    monitor.setTenantId(Monitor.POLICY_TENANT);
    return monitorRepository.save(monitor);
  }

  public static String createSingleTenant(ResourceRepository resourceRepository) {
    Resource resource = podamFactory.manufacturePojo(Resource.class);
    resource.setResourceId(RandomStringUtils.randomAlphabetic(10));
    return resourceRepository.save(resource).getTenantId();
  }

  public static List<String> createMultipleTenants(ResourceRepository resourceRepository) {
    return IntStream.range(0, 5)
        .mapToObj(i -> createSingleTenant(resourceRepository))
        .collect(Collectors.toList());
  }

}
