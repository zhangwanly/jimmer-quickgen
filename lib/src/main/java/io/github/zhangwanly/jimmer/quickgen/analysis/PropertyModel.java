package io.github.zhangwanly.jimmer.quickgen.analysis;

import com.squareup.javapoet.TypeName;

/**
 * Represents a scalar (column-mapped) property for code generation.
 *
 * @param name         Java property name in lowerCamelCase
 * @param columnName   database column name in lower_snake_case
 * @param javaType     JavaPoet type name
 * @param nullable     whether the property is nullable
 * @param isPrimaryKey whether this property is a primary key
 */
public record PropertyModel(
        String name,
        String columnName,
        TypeName javaType,
        boolean nullable,
        boolean isPrimaryKey
) {}
