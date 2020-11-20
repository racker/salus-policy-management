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

package com.rackspace.salus.policy.manage.web.client;

import com.rackspace.salus.policy.manage.web.model.MonitorMetadataPolicyDTO;
import com.rackspace.salus.policy.manage.web.model.MonitorPolicyDTO;
import com.rackspace.salus.telemetry.model.MonitorType;
import com.rackspace.salus.telemetry.model.TargetClassName;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This interface declares a subset of internal REST API calls exposed by the Policy Management
 * service.
 *
 * @see PolicyApiClient
 */
public interface PolicyApi {
  List<MonitorPolicyDTO> getEffectiveMonitorPoliciesForTenant(String tenantId, boolean useCache);
  List<UUID> getEffectiveMonitorPolicyIdsForTenant(String tenantId, boolean includeNullMonitors, boolean useCache);
  List<UUID> getEffectivePolicyMonitorIdsForTenant(String tenantId, boolean useCache);
  List<MonitorMetadataPolicyDTO> getEffectiveMonitorMetadataPolicies(String tenantId, boolean useCache);
  Map<String, MonitorMetadataPolicyDTO> getEffectiveMonitorMetadataMap(
      String tenantId, TargetClassName className, MonitorType monitorType);
  List<String> getDefaultMonitoringZones(String region, boolean useCache);
  void evictEffectiveMonitorMetadataMap(String tenantId, TargetClassName className,
      MonitorType monitorType);
}