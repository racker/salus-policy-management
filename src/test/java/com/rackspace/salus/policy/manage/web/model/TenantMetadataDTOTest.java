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

package com.rackspace.salus.policy.manage.web.model;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.rackspace.salus.telemetry.entities.TenantMetadata;
import java.lang.reflect.Field;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

public class TenantMetadataDTOTest {
  final PodamFactory podamFactory = new PodamFactoryImpl();

  @Test
  public void testFieldsCovered() {
    final TenantMetadata tenantMetadata = podamFactory.manufacturePojo(TenantMetadata.class);

    final TenantMetadataDTO dto = new TenantMetadataDTO(tenantMetadata);

    // First verification approach is to check that all fields are populated with something.
    // This approach makes sure that the verification further down doesn't miss a new field.

    for (Field field : TenantMetadataDTO.class.getDeclaredFields()) {
      field.setAccessible(true);
      final Object value = ReflectionUtils.getField(field, dto);
      assertThat(value, notNullValue());
      if (value instanceof String) {
        assertThat(((String) value), not(isEmptyString()));
      }
    }

    // and next verification is to check populated with correct values

    assertThat(dto.getId(), equalTo(tenantMetadata.getId()));
    assertThat(dto.getTenantId(), equalTo(tenantMetadata.getTenantId()));
    assertThat(dto.getAccountType(), equalTo(tenantMetadata.getAccountType()));
    assertThat(dto.getMetadata(), equalTo(tenantMetadata.getMetadata()));
    assertThat(dto.getCreatedTimestamp(),
        equalTo(DateTimeFormatter.ISO_INSTANT.format(tenantMetadata.getCreatedTimestamp())));
    assertThat(dto.getUpdatedTimestamp(),
        equalTo(DateTimeFormatter.ISO_INSTANT.format(tenantMetadata.getUpdatedTimestamp())));
  }
}
