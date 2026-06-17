package io.github.zhangwanly.jimmer.quickgen.codegen;

import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Builds a {@code @MappedSuperclass} BaseEntity interface using JavaPoet.
 */
public final class BaseEntityBuilder {

    private BaseEntityBuilder() {
    }

    /**
     * Build the BaseEntity interface from the extracted base columns.
     *
     * @param baseColumns columns that belong to BaseEntity
     * @param pkColumns   set of column names that are primary keys
     * @param config      QuickGen configuration
     * @return TypeSpec for the BaseEntity interface
     */
    public static TypeSpec build(List<ColumnModel> baseColumns, java.util.Set<String> pkColumns, QuickGenConfig config) {
        String baseName = config.baseEntityConfig().name();

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(baseName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationFactory.mappedSuperclass());

        for (ColumnModel col : baseColumns) {
            boolean isPk = pkColumns.stream().anyMatch(pk -> pk.equalsIgnoreCase(col.name()));
            MethodSpec method = PropertyMethodBuilder.build(col, isPk, config, false);
            builder.addMethod(method);
        }

        return builder.build();
    }
}
