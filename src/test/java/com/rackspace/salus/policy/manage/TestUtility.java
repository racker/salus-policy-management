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
import com.rackspace.salus.telemetry.entities.TenantMetadata;
import com.rackspace.salus.telemetry.repositories.MonitorRepository;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

public class TestUtility {

  private static PodamFactory podamFactory = new PodamFactoryImpl();

  public static Monitor createPolicyMonitor(MonitorRepository monitorRepository) {
    Monitor monitor = podamFactory.manufacturePojo(Monitor.class);
    monitor.setTenantId(Monitor.POLICY_TENANT);
    return monitorRepository.save(monitor);
  }

  public static String createSingleTenant(TenantMetadataRepository tenantRepository) {
      TenantMetadata tenantMetadata = podamFactory.manufacturePojo(TenantMetadata.class);
      tenantMetadata.setTenantId(RandomStringUtils.randomAlphanumeric(10));
      return tenantRepository.save(tenantMetadata).getTenantId();
  }

  public static List<String> createMultipleTenants(TenantMetadataRepository tenantRepository) {
    return createMultipleTenants(tenantRepository, 5);
  }

  public static List<String> createMultipleTenants(TenantMetadataRepository tenantRepository, int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> createSingleTenant(tenantRepository))
        .collect(Collectors.toList());
  }

  public static String createTenantOfAccountType(TenantMetadataRepository tenantRepository, String accountType) {
    return tenantRepository.save(new TenantMetadata()
        .setAccountType(accountType)
        .setTenantId(RandomStringUtils.randomAlphanumeric(10))
        .setMetadata(Collections.emptyMap())).getTenantId();
  }

  public static List<String> createTenantsOfAccountType(TenantMetadataRepository tenantRepository, int count, String accountType) {
    return IntStream.range(0, count)
        .mapToObj(i -> createTenantOfAccountType(tenantRepository, accountType))
        .collect(Collectors.toList());
  }
}
