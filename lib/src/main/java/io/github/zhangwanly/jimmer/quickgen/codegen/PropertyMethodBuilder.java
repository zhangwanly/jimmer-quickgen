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
     * @param column                the database column model
     * @param isPrimaryKey          whether this column is a primary key
     * @param config                the QuickGen configuration
     * @param addColumnAnnotation   whether to add @Column when the inferred name differs
     * @return a MethodSpec for the property
     */
    public static MethodSpec build(ColumnModel column, boolean isPrimaryKey,
                                   QuickGenConfig config, boolean addColumnAnnotation) {
        String propertyName = NamingConventions.columnToPropertyName(column.name());
        TypeMappingRegistry registry = config.typeMappingRegistry();
        TypeName javaType = registry.resolve(column.name(), column.typeName(), column.nullable());

        return buildScalar(propertyName, column.name(), javaType, column.nullable(),
                isPrimaryKey, config, addColumnAnnotation);
    }

    /**
     * Build a scalar property method from pre-resolved values.
     * Shared by both ColumnModel-based and PropertyModel-based paths.
     */
    static MethodSpec buildScalar(String propertyName, String columnName,
                                  TypeName javaType, boolean nullable, boolean isPrimaryKey,
                                  QuickGenConfig config, boolean addColumnAnnotation) {
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

        if (addColumnAnnotation) {
            String expectedColumnName = NamingConventions.propertyNameToColumnName(propertyName);
            if (!expectedColumnName.equalsIgnoreCase(columnName)) {
                builder.addAnnotation(AnnotationFactory.column(columnName));
            }
        }

        return builder.build();
    }
}
