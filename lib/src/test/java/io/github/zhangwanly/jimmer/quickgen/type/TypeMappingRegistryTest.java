package io.github.zhangwanly.jimmer.quickgen.type;

import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeMappingRegistryTest {

    @Test
    void varcharMapsToString() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.get(String.class), registry.resolve("VARCHAR"));
    }

    @Test
    void integerMapsToInt() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.INT, registry.resolve("INTEGER"));
    }

    @Test
    void bigintMapsToLong() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.LONG, registry.resolve("BIGINT"));
    }

    @Test
    void decimalMapsToBigDecimal() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.get(BigDecimal.class), registry.resolve("DECIMAL"));
    }

    @Test
    void dateMapsToLocalDate() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.get(LocalDate.class), registry.resolve("DATE"));
    }

    @Test
    void timestampMapsToLocalDateTime() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.get(LocalDateTime.class), registry.resolve("TIMESTAMP"));
    }

    @Test
    void timeMapsToLocalTime() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.get(LocalTime.class), registry.resolve("TIME"));
    }

    @Test
    void booleanMapsToBoolean() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.BOOLEAN, registry.resolve("BOOLEAN"));
    }

    @Test
    void unknownTypeFallsBackToString() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.get(String.class), registry.resolve("UNKNOWN_TYPE"));
    }

    @Test
    void caseInsensitive() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.INT, registry.resolve("integer"));
        assertEquals(TypeName.get(String.class), registry.resolve("varchar"));
    }

    @Test
    void overrideTakesPrecedence() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults()
                .register("JSON", java.util.UUID.class);
        assertEquals(TypeName.get(java.util.UUID.class), registry.resolve("JSON"));
    }

    @Test
    void overrideIsCaseInsensitive() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults()
                .register("json", java.util.UUID.class);
        assertEquals(TypeName.get(java.util.UUID.class), registry.resolve("JSON"));
    }

    @Test
    void resolveWithNullableBoxesPrimitives() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        // nullable int → Integer (boxed)
        assertEquals(TypeName.get(Integer.class), registry.resolve("INTEGER", true));
        // non-nullable int → int (primitive)
        assertEquals(TypeName.INT, registry.resolve("INTEGER", false));
    }

    @Test
    void resolveWithNullableKeepsNonPrimitiveTypes() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        // nullable String → String (not boxed, already reference type)
        assertEquals(TypeName.get(String.class), registry.resolve("VARCHAR", true));
    }

    @Test
    void chainingReturnsThis() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        TypeMappingRegistry result = registry.register("A", String.class).register("B", String.class);
        assertSame(registry, result);
    }

    @Test
    void booleanColumnPattern_isDeleted() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        // is_deleted (TINYINT, not null) → boolean
        assertEquals(TypeName.BOOLEAN, registry.resolve("is_deleted", "TINYINT", false));
        // is_deleted (TINYINT, nullable) → Boolean (boxed)
        assertEquals(TypeName.get(Boolean.class), registry.resolve("is_deleted", "TINYINT", true));
    }

    @Test
    void booleanColumnPattern_isEnabled() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertEquals(TypeName.BOOLEAN, registry.resolve("is_enabled", "TINYINT", false));
    }

    @Test
    void booleanColumnPattern_nonMatchingColumn() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        // status (TINYINT, not null) → int (not boolean)
        assertEquals(TypeName.INT, registry.resolve("status", "TINYINT", false));
        // order_num (TINYINT, not null) → int
        assertEquals(TypeName.INT, registry.resolve("order_num", "TINYINT", false));
    }

    @Test
    void booleanColumnPattern_customPatterns() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults()
                .booleanColumnPatterns(List.of("is_*", "has_*"));
        // has_permission (TINYINT) → boolean
        assertEquals(TypeName.BOOLEAN, registry.resolve("has_permission", "TINYINT", false));
        // is_active → boolean
        assertEquals(TypeName.BOOLEAN, registry.resolve("is_active", "TINYINT", false));
    }

    @Test
    void matchesBooleanPattern_variousNames() {
        TypeMappingRegistry registry = TypeMappingRegistry.defaults();
        assertTrue(registry.matchesBooleanPattern("is_deleted"));
        assertTrue(registry.matchesBooleanPattern("is_enabled"));
        assertTrue(registry.matchesBooleanPattern("IS_ACTIVE")); // case insensitive
        assertFalse(registry.matchesBooleanPattern("status"));
        assertFalse(registry.matchesBooleanPattern("order_num"));
        assertFalse(registry.matchesBooleanPattern("deleted"));
    }
}
