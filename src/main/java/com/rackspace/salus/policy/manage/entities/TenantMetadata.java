package com.rackspace.salus.policy.manage.entities;

import com.rackspace.salus.policy.manage.web.model.TenantMetadataDTO;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.validator.constraints.NotBlank;

@Entity
@Table(name = "tenant_metadata")
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Data
public class TenantMetadata {
  @Id
  @GeneratedValue
  @org.hibernate.annotations.Type(type="uuid-char")
  UUID id;

  @NotBlank
  @Column(name = "tenant_id", unique = true)
  String tenantId;

  @NotNull
  @Type(type = "json")
  @Column(columnDefinition = "json")
  Map<String, String> metadata;

  public TenantMetadataDTO toDTO() {
    return new TenantMetadataDTO()
        .setId(id)
        .setTenantId(tenantId)
        .setMetadata(metadata);
  }
}
