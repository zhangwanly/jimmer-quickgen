package io.github.zhangwanly.jimmer.quickgen.analysis;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for naming convention conversions between database (lower_snake_case)
 * and Java (UpperCamelCase for classes, lowerCamelCase for properties).
 */
public final class NamingConventions {

    private NamingConventions() {}

    /**
     * Well-known mappings from singular owning property name to plural inverse name
     * for self-referencing relationships.
     * <p>Example: {@code parent} → {@code children}</p>
     */
    private static final Map<String, String> SELF_REF_INVERSE_NAMES = Map.of(
            "parent", "children",
            "root", "descendants"
    );

    /**
     * Derive the inverse-side property name for a self-referencing relationship.
     * <p>Uses a well-known mapping first (e.g. {@code parent} → {@code children}),
     * then falls back to pluralizing the entity's camelCase name.</p>
     *
     * @param owningPropName the owning-side property name (e.g. "parent")
     * @param entityCamel    the entity name in lowerCamelCase (e.g. "category")
     * @return the inverse-side collection property name
     */
    public static String selfRefInverseName(String owningPropName, String entityCamel) {
        String mapped = SELF_REF_INVERSE_NAMES.get(owningPropName);
        return mapped != null ? mapped : pluralize(entityCamel);
    }

    /**
     * Convert a lower_snake_case table name to an UpperCamelCase entity name.
     * <p>Example: {@code book_store} → {@code BookStore}</p>
     */
    public static String tableToEntityName(String tableName) {
        return toUpperCamelCase(tableName);
    }

    /**
     * Convert a lower_snake_case column name to a lowerCamelCase property name.
     * <p>Example: {@code first_name} → {@code firstName}</p>
     */
    public static String columnToPropertyName(String columnName) {
        return toLowerCamelCase(columnName);
    }

    /**
     * Convert an UpperCamelCase entity name back to lower_snake_case table name.
     * <p>Example: {@code BookStore} → {@code book_store}</p>
     */
    public static String entityToTableName(String entityName) {
        return toLowerSnakeCase(entityName);
    }

    /**
     * Convert a lowerCamelCase property name back to lower_snake_case column name.
     * <p>Example: {@code firstName} → {@code first_name}</p>
     */
    public static String propertyNameToColumnName(String propertyName) {
        return toLowerSnakeCase(propertyName);
    }

    /**
     * Extract the referenced table name from a foreign-key-style column name.
     * <p>Convention: {@code {table_name}_id} → {@code table_name}</p>
     * <p>Example: {@code store_id} → {@code Optional.of("store")}, {@code first_name} → {@code Optional.empty()}</p>
     *
     * @param columnName the column name to inspect
     * @return the referenced table name, or empty if the column doesn't follow the convention
     */
    public static Optional<String> extractReferenceTableName(String columnName) {
        String lower = columnName.toLowerCase();
        if (lower.endsWith("_id") && lower.length() > 3) {
            return Optional.of(lower.substring(0, lower.length() - 3));
        }
        return Optional.empty();
    }

    /**
     * Look up a table reference override for the given table and column.
     * <p>This allows legacy databases with non-standard FK column naming to be
     * correctly resolved. For example, {@code order_info.user_id} may actually
     * reference {@code user_info} rather than {@code user}.</p>
     *
     * @param tableName  the table owning the FK column (lowercase)
     * @param columnName the FK column name (lowercase)
     * @param overrides  the configured overrides
     * @return the actual referenced table name, or empty if no override matches
     */
    public static Optional<String> resolveTableRefOverride(
            String tableName, String columnName, List<TableRefOverride> overrides) {
        String tableLower = tableName.toLowerCase();
        String colLower = columnName.toLowerCase();
        for (TableRefOverride override : overrides) {
            if (override.tableName().equals(tableLower) && override.columnName().equals(colLower)) {
                return Optional.of(override.actualRefTable());
            }
        }
        return Optional.empty();
    }

    /**
     * Derive the inverse-side property name for a non-self-referencing OneToMany relationship.
     * <p>Appends "List" to the lowerCamelCase entity name.
     * Example: {@code book} → {@code bookList}, {@code productDetails} → {@code productDetailsList}</p>
     *
     * @param entityCamel the entity name in lowerCamelCase (e.g. "book", "productSku")
     * @return the inverse-side collection property name (e.g. "bookList", "productSkuList")
     */
    public static String toListPropertyName(String entityCamel) {
        if (entityCamel == null || entityCamel.isEmpty()) return entityCamel;
        return entityCamel + "List";
    }

    /**
     * Result of detecting a {@code {table}{digit}_id} column pattern.
     *
     * @param tableName the referenced table name (lowercase)
     * @param digit     the numeric suffix (e.g. 1, 2, 3)
     */
    public record DigitSuffixRef(String tableName, int digit) {}

    /**
     * Detect if a column name follows the {@code {table}{digit}_id} pattern.
     * <p>Examples:
     * <ul>
     *   <li>{@code category1_id} with tables containing "category" → {@code Optional.of(DigitSuffixRef("category", 1))}</li>
     *   <li>{@code category12_id} with tables containing "category" → {@code Optional.of(DigitSuffixRef("category", 12))}</li>
     *   <li>{@code category_id} (no digit) → {@code Optional.empty()}</li>
     *   <li>{@code unknown1_id} with no "unknown" table → {@code Optional.empty()}</li>
     * </ul>
     *
     * @param columnName the column name to inspect
     * @param tableNames set of known table names (lowercase)
     * @return the parsed reference, or empty if not a valid digit-suffix pattern
     */
    public static Optional<DigitSuffixRef> extractDigitSuffixRef(String columnName, Set<String> tableNames) {
        String lower = columnName.toLowerCase();
        if (!lower.endsWith("_id") || lower.length() <= 3) return Optional.empty();

        String stripped = lower.substring(0, lower.length() - 3); // e.g. "category1"

        // Scan backwards to find trailing digits
        int i = stripped.length() - 1;
        while (i >= 0 && Character.isDigit(stripped.charAt(i))) {
            i--;
        }

        // Must have at least one digit
        if (i >= stripped.length() - 1) return Optional.empty();

        String prefix = stripped.substring(0, i + 1); // e.g. "category"
        if (prefix.isEmpty()) return Optional.empty();

        int digit = Integer.parseInt(stripped.substring(i + 1));

        if (!tableNames.contains(prefix)) return Optional.empty();

        return Optional.of(new DigitSuffixRef(prefix, digit));
    }

    /**
     * Pluralize an English name following common rules:
     * <ul>
     *   <li>Words ending in 'y' (preceded by consonant) → 'ies' (e.g., category → categories)</li>
     *   <li>Words ending in 's', 'x', 'z', 'ch', 'sh' → 'es' (e.g., box → boxes)</li>
     *   <li>Otherwise → 's' (e.g., book → books)</li>
     * </ul>
     */
    public static String pluralize(String name) {
        if (name == null || name.isEmpty()) return name;

        String lower = name.toLowerCase();

        // Words ending in consonant + y → ies
        if (lower.endsWith("y") && lower.length() > 1) {
            char before = lower.charAt(lower.length() - 2);
            if (!isVowel(before)) {
                return name.substring(0, name.length() - 1) + "ies";
            }
        }

        // Words ending in s, x, z, ch, sh → es
        if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
                || lower.endsWith("ch") || lower.endsWith("sh")) {
            return name + "es";
        }

        return name + "s";
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(Character.toLowerCase(c)) >= 0;
    }

    private static String toUpperCamelCase(String snakeCase) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private static String toLowerCamelCase(String snakeCase) {
        String upper = toUpperCamelCase(snakeCase);
        if (upper.isEmpty()) return upper;
        return Character.toLowerCase(upper.charAt(0)) + upper.substring(1);
    }

    private static String toLowerSnakeCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!sb.isEmpty()) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
