package io.github.zhangwanly.jimmer.quickgen.dialect;

import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;
import com.squareup.javapoet.TypeName;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.UUID;

/**
 * PostgreSQL-specific type mappings.
 *
 * <p>Registers types that are specific to PostgreSQL and not covered by
 * the standard SQL type mappings in {@link TypeMappingRegistry}.</p>
 *
 * <p>Types already in default mappings (not re-registered here):
 * INTEGER, BIGINT, REAL, DOUBLE PRECISION, NUMERIC,
 * VARCHAR, CHAR, TEXT, BOOLEAN, DATE, TIME, TIMESTAMP, JSON, JSONB, BYTEA</p>
 */
final class PostgresDialect implements Dialect {

    @Override
    public String name() {
        return "PostgreSQL";
    }

    @Override
    public void applyTypeMappings(TypeMappingRegistry registry) {
        // PostgreSQL serial (auto-increment) types
        registry.registerDialectType("SERIAL", TypeName.INT);
        registry.registerDialectType("SERIAL4", TypeName.INT);
        registry.registerDialectType("SERIAL8", TypeName.LONG);
        registry.registerDialectType("BIGSERIAL", TypeName.LONG);
        registry.registerDialectType("SMALLSERIAL", TypeName.INT);

        // PostgreSQL integer aliases
        registry.registerDialectType("INT2", TypeName.INT);
        registry.registerDialectType("INT4", TypeName.INT);
        registry.registerDialectType("INT8", TypeName.LONG);

        // PostgreSQL floating-point aliases
        registry.registerDialectType("FLOAT4", TypeName.FLOAT);
        registry.registerDialectType("FLOAT8", TypeName.DOUBLE);

        // PostgreSQL-specific types
        registry.registerDialectType("UUID", TypeName.get(UUID.class));
        registry.registerDialectType("BYTEA", TypeName.get(byte[].class));
        registry.registerDialectType("INET", TypeName.get(InetAddress.class));
        registry.registerDialectType("CIDR", TypeName.get(String.class));
        registry.registerDialectType("MACADDR", TypeName.get(String.class));
        registry.registerDialectType("XML", TypeName.get(String.class));
        registry.registerDialectType("MONEY", TypeName.get(BigDecimal.class));

        // PostgreSQL timezone-aware types
        registry.registerDialectType("TIMESTAMPTZ", TypeName.get(OffsetDateTime.class));
        registry.registerDialectType("TIMETZ", TypeName.get(OffsetTime.class));

        // PostgreSQL interval type
        registry.registerDialectType("INTERVAL", TypeName.get(Duration.class));
    }
}
