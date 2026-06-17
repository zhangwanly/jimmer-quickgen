package io.github.zhangwanly.jimmer.quickgen.analysis;

import java.util.List;

/**
 * Represents a fully resolved entity model ready for code generation.
 *
 * @param tableName        database table name in lower_snake_case
 * @param entityName       Java interface name in UpperCamelCase
 * @param scalars          scalar properties (excluding BaseEntity columns)
 * @param associations     association properties
 * @param extendsBaseEntity whether this entity extends BaseEntity
 */
public record EntityModel(
        String tableName,
        String entityName,
        List<PropertyModel> scalars,
        List<AssociationModel> associations,
        boolean extendsBaseEntity
) {
    public EntityModel {
        scalars = List.copyOf(scalars);
        associations = List.copyOf(associations);
    }
}
