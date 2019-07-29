package com.rackspace.salus.policy.manage.entities;

import com.rackspace.salus.policy.manage.web.model.TenantMetadataDTO;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;
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

  @Column(name = "account_type")
  String accountType;

  @NotNull
  @Type(type = "json")
  @Column(columnDefinition = "text")
  Map<String, String> metadata;

  @CreationTimestamp
  @Column(name="created_timestamp")
  Instant createdTimestamp;

  @UpdateTimestamp
  @Column(name="updated_timestamp")
  Instant updatedTimestamp;

  public TenantMetadataDTO toDTO() {
    return new TenantMetadataDTO()
        .setId(id)
        .setTenantId(tenantId)
        .setMetadata(metadata)
        .setCreatedTimestamp(DateTimeFormatter.ISO_INSTANT.format(createdTimestamp))
        .setUpdatedTimestamp(DateTimeFormatter.ISO_INSTANT.format(updatedTimestamp));
  }
}
