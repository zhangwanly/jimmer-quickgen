package io.github.zhangwanly.jimmer.quickgen.dialect;

import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;
import com.squareup.javapoet.TypeName;

import java.time.Year;

/**
 * MySQL-specific type mappings.
 *
 * <p>Registers types that are specific to MySQL and not covered by
 * the standard SQL type mappings in {@link TypeMappingRegistry}.</p>
 *
 * <p>Types already in default mappings (not re-registered here):
 * TINYINT, SMALLINT, INT, BIGINT, FLOAT, DOUBLE, DECIMAL,
 * VARCHAR, CHAR, TEXT, BLOB, DATE, TIME, TIMESTAMP, DATETIME, BIT, JSON</p>
 */
final class MysqlDialect implements Dialect {

    @Override
    public String name() {
        return "MySQL";
    }

    @Override
    public void applyTypeMappings(TypeMappingRegistry registry) {
        // MySQL-specific integer types
        registry.registerDialectType("MEDIUMINT", TypeName.INT);

        // MySQL-specific text types
        registry.registerDialectType("TINYTEXT", TypeName.get(String.class));
        registry.registerDialectType("MEDIUMTEXT", TypeName.get(String.class));
        registry.registerDialectType("LONGTEXT", TypeName.get(String.class));

        // MySQL-specific binary types
        registry.registerDialectType("TINYBLOB", TypeName.get(byte[].class));
        registry.registerDialectType("MEDIUMBLOB", TypeName.get(byte[].class));
        registry.registerDialectType("LONGBLOB", TypeName.get(byte[].class));

        // MySQL-specific types
        registry.registerDialectType("ENUM", TypeName.get(String.class));
        registry.registerDialectType("SET", TypeName.get(String.class));
        registry.registerDialectType("YEAR", TypeName.get(Year.class));
    }
}
