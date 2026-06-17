package io.github.zhangwanly.jimmer.quickgen.dialect;

import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DialectTest {

    // ── Dialect detection (fromProductName) ──────────────────────────────

    @Test
    void fromProductName_mysql() {
        Dialect dialect = Dialect.fromProductName("MySQL");
        assertEquals("MySQL", dialect.name());
        assertInstanceOf(MysqlDialect.class, dialect);
    }

    @Test
    void fromProductName_mysqlCaseInsensitive() {
        Dialect dialect = Dialect.fromProductName("mysql 8.0");
        assertEquals("MySQL", dialect.name());
    }

    @Test
    void fromProductName_postgresql() {
        Dialect dialect = Dialect.fromProductName("PostgreSQL");
        assertEquals("PostgreSQL", dialect.name());
        assertInstanceOf(PostgresDialect.class, dialect);
    }

    @Test
    void fromProductName_postgresShortName() {
        Dialect dialect = Dialect.fromProductName("Postgres");
        assertEquals("PostgreSQL", dialect.name());
    }

    @Test
    void fromProductName_h2ReturnsGeneric() {
        Dialect dialect = Dialect.fromProductName("H2");
        assertEquals("Generic", dialect.name());
        assertInstanceOf(GenericDialect.class, dialect);
    }

    @Test
    void fromProductName_unknownReturnsGeneric() {
        Dialect dialect = Dialect.fromProductName("SQLite");
        assertEquals("Generic", dialect.name());
    }

    @Test
    void fromProductName_nullReturnsGeneric() {
        Dialect dialect = Dialect.fromProductName(null);
        assertEquals("Generic", dialect.name());
    }

    // ── MySQL type mappings ──────────────────────────────────────────────

    @Test
    void mysql_mediumintMapsToInt() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new MysqlDialect().applyTypeMappings(registry);
        assertEquals(TypeName.INT, registry.resolve("MEDIUMINT"));
    }

    @Test
    void mysql_textTypesMapToString() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new MysqlDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(String.class), registry.resolve("TINYTEXT"));
        assertEquals(TypeName.get(String.class), registry.resolve("MEDIUMTEXT"));
        assertEquals(TypeName.get(String.class), registry.resolve("LONGTEXT"));
    }

    @Test
    void mysql_blobTypesMapToByteArray() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new MysqlDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(byte[].class), registry.resolve("TINYBLOB"));
        assertEquals(TypeName.get(byte[].class), registry.resolve("MEDIUMBLOB"));
        assertEquals(TypeName.get(byte[].class), registry.resolve("LONGBLOB"));
    }

    @Test
    void mysql_enumAndSetMapToString() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new MysqlDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(String.class), registry.resolve("ENUM"));
        assertEquals(TypeName.get(String.class), registry.resolve("SET"));
    }

    @Test
    void mysql_yearMapsToYear() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new MysqlDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(Year.class), registry.resolve("YEAR"));
    }

    // ── PostgreSQL type mappings ────────────────────────────────────────

    @Test
    void pg_serialTypesMapToInt() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.INT, registry.resolve("SERIAL"));
        assertEquals(TypeName.INT, registry.resolve("SERIAL4"));
        assertEquals(TypeName.INT, registry.resolve("SMALLSERIAL"));
    }

    @Test
    void pg_bigSerialTypesMapToLong() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.LONG, registry.resolve("BIGSERIAL"));
        assertEquals(TypeName.LONG, registry.resolve("SERIAL8"));
    }

    @Test
    void pg_intAliases() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.INT, registry.resolve("INT2"));
        assertEquals(TypeName.INT, registry.resolve("INT4"));
        assertEquals(TypeName.LONG, registry.resolve("INT8"));
    }

    @Test
    void pg_floatAliases() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.FLOAT, registry.resolve("FLOAT4"));
        assertEquals(TypeName.DOUBLE, registry.resolve("FLOAT8"));
    }

    @Test
    void pg_uuidMapsToUUID() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(UUID.class), registry.resolve("UUID"));
    }

    @Test
    void pg_byteaMapsToByteArray() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(byte[].class), registry.resolve("BYTEA"));
    }

    @Test
    void pg_inetMapsToInetAddress() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(InetAddress.class), registry.resolve("INET"));
    }

    @Test
    void pg_networkAndXmlMapToString() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(String.class), registry.resolve("CIDR"));
        assertEquals(TypeName.get(String.class), registry.resolve("MACADDR"));
        assertEquals(TypeName.get(String.class), registry.resolve("XML"));
    }

    @Test
    void pg_moneyMapsToBigDecimal() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(BigDecimal.class), registry.resolve("MONEY"));
    }

    @Test
    void pg_timezoneTypes() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(OffsetDateTime.class), registry.resolve("TIMESTAMPTZ"));
        assertEquals(TypeName.get(OffsetTime.class), registry.resolve("TIMETZ"));
    }

    @Test
    void pg_intervalMapsToDuration() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new PostgresDialect().applyTypeMappings(registry);
        assertEquals(TypeName.get(Duration.class), registry.resolve("INTERVAL"));
    }

    // ── Priority: user override > dialect override > default ─────────────

    @Test
    void userOverrideBeatsDialectOverride() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        // Dialect maps ENUM → String, but user wants a custom type
        new MysqlDialect().applyTypeMappings(registry);
        registry.register("ENUM", Integer.class);
        assertEquals(TypeName.get(Integer.class), registry.resolve("ENUM"));
    }

    @Test
    void dialectOverrideBeatsDefault() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        // Default has no MEDIUMINT mapping, dialect adds it
        new MysqlDialect().applyTypeMappings(registry);
        assertEquals(TypeName.INT, registry.resolve("MEDIUMINT"));
    }

    @Test
    void defaultMappingsStillWorkAfterDialectApplied() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new MysqlDialect().applyTypeMappings(registry);
        // Default mappings should still work
        assertEquals(TypeName.get(String.class), registry.resolve("VARCHAR"));
        assertEquals(TypeName.INT, registry.resolve("INTEGER"));
        assertEquals(TypeName.LONG, registry.resolve("BIGINT"));
    }

    // ── GenericDialect ──────────────────────────────────────────────────

    @Test
    void genericDialectDoesNotOverrideDefaults() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        new GenericDialect().applyTypeMappings(registry);
        // Defaults should remain intact
        assertEquals(TypeName.get(String.class), registry.resolve("VARCHAR"));
        assertEquals(TypeName.INT, registry.resolve("INTEGER"));
    }
}
