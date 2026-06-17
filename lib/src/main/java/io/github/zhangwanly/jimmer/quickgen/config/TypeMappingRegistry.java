package io.github.zhangwanly.jimmer.quickgen.config;

import com.squareup.javapoet.TypeName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Three-tier type mapping registry: default mappings + dialect overrides + user overrides.
 *
 * <p>Lookup order: column name pattern > user override > dialect override > default mapping > fallback String.</p>
 */
public final class TypeMappingRegistry {

    private static final Map<String, TypeName> DEFAULT_MAPPINGS = new ConcurrentHashMap<>();

    /**
     * Default column name patterns that should map to boolean.
     * <p>Pattern syntax: {@code *} matches any suffix. Example: {@code is_*} matches {@code is_deleted}.</p>
     */
    private static final List<String> DEFAULT_BOOLEAN_COLUMN_PATTERNS = List.of("is_*");

    static {
        // String types
        DEFAULT_MAPPINGS.put("VARCHAR", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("CHAR", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("NCHAR", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("NVARCHAR", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("LONGVARCHAR", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("LONGNVARCHAR", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("CLOB", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("NCLOB", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("TEXT", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("JSON", TypeName.get(String.class));
        DEFAULT_MAPPINGS.put("JSONB", TypeName.get(String.class));

        // Integer types
        DEFAULT_MAPPINGS.put("TINYINT", TypeName.INT);
        DEFAULT_MAPPINGS.put("SMALLINT", TypeName.INT);
        DEFAULT_MAPPINGS.put("INTEGER", TypeName.INT);
        DEFAULT_MAPPINGS.put("INT", TypeName.INT);

        // Long types
        DEFAULT_MAPPINGS.put("BIGINT", TypeName.LONG);

        // Floating point
        DEFAULT_MAPPINGS.put("REAL", TypeName.FLOAT);
        DEFAULT_MAPPINGS.put("FLOAT", TypeName.DOUBLE);
        DEFAULT_MAPPINGS.put("DOUBLE", TypeName.DOUBLE);

        // Decimal
        DEFAULT_MAPPINGS.put("DECIMAL", TypeName.get(BigDecimal.class));
        DEFAULT_MAPPINGS.put("NUMERIC", TypeName.get(BigDecimal.class));

        // Date/Time
        DEFAULT_MAPPINGS.put("DATE", TypeName.get(LocalDate.class));
        DEFAULT_MAPPINGS.put("TIME", TypeName.get(LocalTime.class));
        DEFAULT_MAPPINGS.put("TIMESTAMP", TypeName.get(LocalDateTime.class));
        DEFAULT_MAPPINGS.put("DATETIME", TypeName.get(LocalDateTime.class));

        // Boolean
        DEFAULT_MAPPINGS.put("BOOLEAN", TypeName.BOOLEAN);
        DEFAULT_MAPPINGS.put("BIT", TypeName.BOOLEAN);

        // Binary
        DEFAULT_MAPPINGS.put("BINARY", TypeName.get(byte[].class));
        DEFAULT_MAPPINGS.put("VARBINARY", TypeName.get(byte[].class));
        DEFAULT_MAPPINGS.put("LONGVARBINARY", TypeName.get(byte[].class));
        DEFAULT_MAPPINGS.put("BLOB", TypeName.get(byte[].class));
    }

    private final Map<String, TypeName> overrides;
    private final Map<String, TypeName> dialectOverrides;
    private final List<String> booleanColumnPatterns;

    private TypeMappingRegistry() {
        this.overrides = new ConcurrentHashMap<>();
        this.dialectOverrides = new ConcurrentHashMap<>();
        this.booleanColumnPatterns = new ArrayList<>(DEFAULT_BOOLEAN_COLUMN_PATTERNS);
    }

    public static TypeMappingRegistry defaults() {
        return new TypeMappingRegistry();
    }

    /**
     * Register a custom type mapping that overrides the default.
     *
     * @param dbTypeName database column type name (case-insensitive, e.g. "JSON", "UUID")
     * @param javaType   the Java class to map to
     * @return this registry for chaining
     */
    public TypeMappingRegistry register(String dbTypeName, Class<?> javaType) {
        overrides.put(dbTypeName.toUpperCase(), TypeName.get(javaType));
        return this;
    }

    /**
     * Register a custom type mapping using a JavaPoet TypeName.
     */
    public TypeMappingRegistry register(String dbTypeName, TypeName javaType) {
        overrides.put(dbTypeName.toUpperCase(), javaType);
        return this;
    }

    /**
     * Register a dialect-specific type mapping.
     * <p>Dialect overrides have lower priority than user overrides.</p>
     *
     * @param dbTypeName database column type name (case-insensitive)
     * @param javaType   the Java class to map to
     * @return this registry for chaining
     */
    public TypeMappingRegistry registerDialectType(String dbTypeName, TypeName javaType) {
        dialectOverrides.put(dbTypeName.toUpperCase(), javaType);
        return this;
    }

    /**
     * Set the column name patterns that should map to boolean.
     * <p>Pattern syntax: {@code *} matches any suffix. Example: {@code is_*}.</p>
     *
     * @param patterns list of patterns (e.g. ["is_*", "has_*"])
     * @return this registry for chaining
     */
    public TypeMappingRegistry booleanColumnPatterns(List<String> patterns) {
        this.booleanColumnPatterns.clear();
        this.booleanColumnPatterns.addAll(patterns);
        return this;
    }

    /**
     * Resolve a database type name to a JavaPoet TypeName.
     *
     * @param dbTypeName JDBC type name (e.g. "VARCHAR", "BIGINT")
     * @return the resolved TypeName, or String as fallback
     */
    public TypeName resolve(String dbTypeName) {
        String key = dbTypeName.toUpperCase();
        // 1. User override (highest priority)
        TypeName result = overrides.get(key);
        if (result != null) return result;
        // 2. Dialect override
        result = dialectOverrides.get(key);
        if (result != null) return result;
        // 3. Default mapping
        result = DEFAULT_MAPPINGS.get(key);
        if (result != null) return result;
        // Fallback
        return TypeName.get(String.class);
    }

    /**
     * Resolve a database type name, using a boxed type for nullable primitives.
     *
     * @param dbTypeName JDBC type name
     * @param nullable   whether the column is nullable
     * @return the resolved TypeName (boxed if nullable + primitive)
     */
    public TypeName resolve(String dbTypeName, boolean nullable) {
        TypeName type = resolve(dbTypeName);
        if (nullable && type.isPrimitive()) {
            return type.box();
        }
        return type;
    }

    /**
     * Resolve a database type, considering column name patterns for boolean mapping.
     * <p>Lookup order: column name pattern match > type-based mapping.</p>
     *
     * @param columnName the database column name (e.g. "is_deleted")
     * @param dbTypeName JDBC type name (e.g. "TINYINT")
     * @param nullable   whether the column is nullable
     * @return the resolved TypeName
     */
    public TypeName resolve(String columnName, String dbTypeName, boolean nullable) {
        // Check column name patterns first
        if (matchesBooleanPattern(columnName)) {
            return nullable ? TypeName.get(Boolean.class) : TypeName.BOOLEAN;
        }
        return resolve(dbTypeName, nullable);
    }

    /**
     * Check if a column name matches any configured boolean pattern.
     *
     * @param columnName the database column name
     * @return true if the column name matches a boolean pattern
     */
    public boolean matchesBooleanPattern(String columnName) {
        String lower = columnName.toLowerCase();
        for (String pattern : booleanColumnPatterns) {
            String prefix = pattern.toLowerCase();
            if (prefix.endsWith("*")) {
                prefix = prefix.substring(0, prefix.length() - 1);
                if (lower.startsWith(prefix)) {
                    return true;
                }
            } else if (lower.equals(prefix)) {
                return true;
            }
        }
        return false;
    }
}
