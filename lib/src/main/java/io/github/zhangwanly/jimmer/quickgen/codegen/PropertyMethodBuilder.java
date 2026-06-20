package io.github.zhangwanly.jimmer.quickgen.codegen;

import io.github.zhangwanly.jimmer.quickgen.analysis.NamingConventions;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;

/**
 * Builds JavaPoet MethodSpec for scalar (column-mapped) properties.
 */
public final class PropertyMethodBuilder {

    private PropertyMethodBuilder() {}

    /**
     * Build a method spec for a scalar property.
     *
     * @param column       the database column model
     * @param isPrimaryKey whether this column is a primary key
     * @param config       the QuickGen configuration
     * @return a MethodSpec for the property
     */
    public static MethodSpec build(ColumnModel column, boolean isPrimaryKey, QuickGenConfig config) {
        String propertyName = NamingConventions.columnToPropertyName(column.name());
        TypeMappingRegistry registry = config.typeMappingRegistry();
        TypeName javaType = registry.resolve(column.name(), column.typeName(), column.nullable());

        return buildScalar(propertyName, column.name(), javaType, column.nullable(),
                isPrimaryKey, config);
    }

    /**
     * Build a scalar property method from pre-resolved values.
     * Shared by both ColumnModel-based and PropertyModel-based paths.
     */
    static MethodSpec buildScalar(String propertyName, String columnName,
                                  TypeName javaType, boolean nullable, boolean isPrimaryKey,
                                  QuickGenConfig config) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(propertyName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(javaType);

        if (isPrimaryKey) {
            builder.addAnnotation(AnnotationFactory.id());
            builder.addAnnotation(AnnotationFactory.generatedValue(config.generationType()));
        }

        if (nullable && !isPrimaryKey) {
            builder.addAnnotation(AnnotationFactory.nullable());
        }

        String expectedColumnName = NamingConventions.propertyNameToColumnName(propertyName);
        if (!expectedColumnName.equalsIgnoreCase(columnName) || hasJavaBeanAmbiguity(propertyName, javaType)) {
            builder.addAnnotation(AnnotationFactory.column(columnName));
        }

        return builder.build();
    }

    /**
     * Check if the property name has ambiguity with JavaBean naming conventions,
     * meaning Jimmer's inferred column name might differ from the actual DB column.
     * <p>Ambiguous cases:</p>
     * <ul>
     *     <li>Boolean "is" prefix: {@code isDeleted()} → Jimmer strips "is" → column {@code deleted},
     *         but actual DB column is {@code is_deleted}</li>
     *     <li>"get"/"set" prefix (accessor reserved words): {@code getType()} → Jimmer strips "get"
     *         → column {@code type}, but actual DB column is {@code get_type}</li>
     *     <li>Consecutive uppercase (abbreviation): e.g. {@code URL} → Jimmer might produce
     *         {@code u_r_l} instead of {@code url}</li>
     * </ul>
     */
    static boolean hasJavaBeanAmbiguity(String propertyName, TypeName javaType) {
        // 1. Boolean "is" prefix
        boolean isBoolean = TypeName.BOOLEAN.equals(javaType)
                || TypeName.get(Boolean.class).equals(javaType);
        if (isBoolean && propertyName.length() > 2
                && propertyName.startsWith("is")
                && Character.isUpperCase(propertyName.charAt(2))) {
            return true;
        }
        // 2. "get"/"set" prefix (JavaBean accessor reserved words)
        if (hasAccessorPrefix(propertyName)) {
            return true;
        }
        // 3. Consecutive uppercase letters (abbreviation in property name)
        for (int i = 1; i < propertyName.length(); i++) {
            if (Character.isUpperCase(propertyName.charAt(i))
                    && Character.isUpperCase(propertyName.charAt(i - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the property name starts with "get" or "set" followed by an uppercase letter,
     * which are JavaBean accessor prefixes that Jimmer may strip when inferring column names.
     */
    private static boolean hasAccessorPrefix(String propertyName) {
        if (propertyName.startsWith("get") && propertyName.length() > 3
                && Character.isUpperCase(propertyName.charAt(3))) {
            return true;
        }
        if (propertyName.startsWith("set") && propertyName.length() > 3
                && Character.isUpperCase(propertyName.charAt(3))) {
            return true;
        }
        return false;
    }
}
