package io.github.zhangwanly.jimmer.quickgen.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.zhangwanly.jimmer.quickgen.analysis.AssociationModel;
import io.github.zhangwanly.jimmer.quickgen.analysis.EntityModel;
import io.github.zhangwanly.jimmer.quickgen.analysis.NamingConventions;
import io.github.zhangwanly.jimmer.quickgen.analysis.PropertyModel;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds a Jimmer {@code @Entity} interface TypeSpec from an EntityModel.
 */
public final class EntityBuilder {

    private EntityBuilder() {
    }

    /**
     * Build an @Entity interface from a fully resolved EntityModel.
     * Properties are emitted in table column definition order.
     */
    public static TypeSpec build(EntityModel entityModel, TableModel table, Set<String> baseColumnNames, QuickGenConfig config) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(entityModel.entityName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationFactory.entity());

        if (entityModel.extendsBaseEntity()) {
            ClassName baseEntityClass = ClassName.get(config.basePackage(), config.baseEntityConfig().name());
            builder.addSuperinterface(baseEntityClass);
        }

        // Build map from FK column name to owning-side association
        Map<String, AssociationModel> owningAssocByJoinColumn = new LinkedHashMap<>();
        Set<AssociationModel> processedAssocs = new HashSet<>();
        for (AssociationModel assoc : entityModel.associations()) {
            switch (assoc) {
                case AssociationModel.ManyToOneAssoc m -> {
                    owningAssocByJoinColumn.put(m.joinColumnName(), m);
                    processedAssocs.add(m);
                }
                case AssociationModel.OneToOneOwningAssoc o -> {
                    owningAssocByJoinColumn.put(o.joinColumnName(), o);
                    processedAssocs.add(o);
                }
                default -> {}
            }
        }

        // Build map from scalar property name to PropertyModel
        Map<String, PropertyModel> scalarByColumnName = new LinkedHashMap<>();
        for (PropertyModel prop : entityModel.scalars()) {
            scalarByColumnName.put(prop.columnName(), prop);
        }

        // Iterate table columns in definition order
        for (ColumnModel col : table.columns()) {
            if (baseColumnNames.stream().anyMatch(bc -> bc.equalsIgnoreCase(col.name()))) {
                continue;
            }

            AssociationModel owningAssoc = owningAssocByJoinColumn.get(col.name());
            if (owningAssoc != null) {
                builder.addMethod(buildAssociationMethod(owningAssoc, config));
            } else {
                PropertyModel prop = scalarByColumnName.get(col.name());
                if (prop != null) {
                    builder.addMethod(buildScalarMethod(prop, config));
                }
            }
        }

        // Emit remaining associations (OneToMany, OneToOne inverse, ManyToMany)
        for (AssociationModel assoc : entityModel.associations()) {
            if (!processedAssocs.contains(assoc)) {
                builder.addMethod(buildAssociationMethod(assoc, config));
            }
        }

        return builder.build();
    }

    /**
     * Build an @Entity interface for the given table (Phase 1 simple mode: no associations).
     * Used by tests for isolated scalar property generation.
     */
    public static TypeSpec buildSimple(TableModel table, QuickGenConfig config) {
        String entityName = NamingConventions.tableToEntityName(table.tableName());

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationFactory.entity());

        for (ColumnModel column : table.columns()) {
            boolean isPk = table.isPrimaryKeyColumn(column.name());
            MethodSpec method = PropertyMethodBuilder.build(column, isPk, config, true);
            builder.addMethod(method);
        }

        return builder.build();
    }

    private static MethodSpec buildScalarMethod(PropertyModel prop, QuickGenConfig config) {
        return PropertyMethodBuilder.buildScalar(
                prop.name(), prop.columnName(), prop.javaType(),
                prop.nullable(), prop.isPrimaryKey(), config, true);
    }

    private static MethodSpec buildAssociationMethod(AssociationModel assoc, QuickGenConfig config) {
        ClassName targetClass = ClassName.get(config.basePackage(), assoc.targetEntityName());

        return switch (assoc) {
            case AssociationModel.ManyToOneAssoc a -> buildManyToOneMethod(a, targetClass);
            case AssociationModel.OneToManyAssoc a -> buildOneToManyMethod(a, targetClass);
            case AssociationModel.OneToOneOwningAssoc a -> buildOneToOneOwningMethod(a, targetClass);
            case AssociationModel.OneToOneInverseAssoc a -> buildOneToOneInverseMethod(a, targetClass);
            case AssociationModel.ManyToManyOwningAssoc a -> buildManyToManyOwningMethod(a, targetClass);
            case AssociationModel.ManyToManyInverseAssoc a -> buildManyToManyInverseMethod(a, targetClass);
        };
    }

    private static MethodSpec buildManyToOneMethod(AssociationModel.ManyToOneAssoc assoc, ClassName targetClass) {
        return MethodSpec.methodBuilder(assoc.propertyName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(targetClass)
                .addAnnotations(AnnotationFactory.manyToOne(assoc.joinColumnName(), assoc.nullable()))
                .build();
    }

    private static MethodSpec buildOneToManyMethod(AssociationModel.OneToManyAssoc assoc, ClassName targetClass) {
        TypeName listType = ParameterizedTypeName.get(
                ClassName.get(java.util.List.class), targetClass);
        return MethodSpec.methodBuilder(assoc.propertyName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listType)
                .addAnnotations(AnnotationFactory.oneToMany(assoc.mappedBy()))
                .build();
    }

    private static MethodSpec buildOneToOneOwningMethod(AssociationModel.OneToOneOwningAssoc assoc, ClassName targetClass) {
        return MethodSpec.methodBuilder(assoc.propertyName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(targetClass)
                .addAnnotations(AnnotationFactory.oneToOneOwning(assoc.joinColumnName(), assoc.nullable()))
                .build();
    }

    private static MethodSpec buildOneToOneInverseMethod(AssociationModel.OneToOneInverseAssoc assoc, ClassName targetClass) {
        return MethodSpec.methodBuilder(assoc.propertyName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(targetClass)
                .addAnnotations(AnnotationFactory.oneToOneInverse(assoc.mappedBy(), assoc.nullable()))
                .build();
    }

    private static MethodSpec buildManyToManyOwningMethod(AssociationModel.ManyToManyOwningAssoc assoc, ClassName targetClass) {
        TypeName listType = ParameterizedTypeName.get(
                ClassName.get(java.util.List.class), targetClass);
        return MethodSpec.methodBuilder(assoc.propertyName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listType)
                .addAnnotations(AnnotationFactory.manyToManyOwning(
                        assoc.joinTableName(), assoc.joinColumnName(), assoc.inverseJoinColumnName()))
                .build();
    }

    private static MethodSpec buildManyToManyInverseMethod(AssociationModel.ManyToManyInverseAssoc assoc, ClassName targetClass) {
        TypeName listType = ParameterizedTypeName.get(
                ClassName.get(java.util.List.class), targetClass);
        return MethodSpec.methodBuilder(assoc.propertyName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listType)
                .addAnnotations(AnnotationFactory.manyToManyInverse(assoc.mappedBy()))
                .build();
    }
}
