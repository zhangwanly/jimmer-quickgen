package io.github.zhangwanly.jimmer.quickgen.codegen;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized factory for building Jimmer annotation specs using JavaPoet.
 */
public final class AnnotationFactory {

    private static final ClassName ENTITY = ClassName.get(Entity.class);
    private static final ClassName MAPPED_SUPERCLASS = ClassName.get(MappedSuperclass.class);
    private static final ClassName ID = ClassName.get(Id.class);
    private static final ClassName GENERATED_VALUE = ClassName.get(GeneratedValue.class);
    private static final ClassName GENERATION_TYPE = ClassName.get(GenerationType.class);
    private static final ClassName COLUMN = ClassName.get(Column.class);
    private static final ClassName TABLE = ClassName.get(Table.class);
    private static final ClassName MANY_TO_ONE = ClassName.get(ManyToOne.class);
    private static final ClassName ONE_TO_MANY = ClassName.get(OneToMany.class);
    private static final ClassName ONE_TO_ONE = ClassName.get(OneToOne.class);
    private static final ClassName MANY_TO_MANY = ClassName.get(ManyToMany.class);
    private static final ClassName JOIN_COLUMN = ClassName.get(JoinColumn.class);
    private static final ClassName JOIN_TABLE = ClassName.get(JoinTable.class);
    private static final ClassName NULLABLE = ClassName.get(Nullable.class);
    private static final ClassName FOREIGN_KEY_TYPE = ClassName.get(ForeignKeyType.class);

    private AnnotationFactory() {}

    public static AnnotationSpec entity() {
        return AnnotationSpec.builder(ENTITY).build();
    }

    public static AnnotationSpec mappedSuperclass() {
        return AnnotationSpec.builder(MAPPED_SUPERCLASS).build();
    }

    public static AnnotationSpec id() {
        return AnnotationSpec.builder(ID).build();
    }

    public static AnnotationSpec generatedValue(GenerationType strategy) {
        return AnnotationSpec.builder(GENERATED_VALUE)
                .addMember("strategy", "$T.$L", GENERATION_TYPE, strategy.name())
                .build();
    }

    public static AnnotationSpec column(String name) {
        return AnnotationSpec.builder(COLUMN)
                .addMember("name", "$S", name.toLowerCase())
                .build();
    }

    public static AnnotationSpec table(String name, String schema) {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(TABLE)
                .addMember("name", "$S", name.toLowerCase());
        if (schema != null && !schema.isBlank()) {
            builder.addMember("schema", "$S", schema);
        }
        return builder.build();
    }

    public static List<AnnotationSpec> manyToOne(String joinColumnName, boolean nullable) {
        List<AnnotationSpec> annotations = new ArrayList<>();
        // FAKE foreign key requires the property to always be @Nullable;
        // use inputNotNull=true to enforce non-null at save time when DB column is NOT NULL.
        annotations.add(nullable());
        AnnotationSpec.Builder manyToOneBuilder = AnnotationSpec.builder(MANY_TO_ONE);
        if (!nullable) {
            manyToOneBuilder.addMember("inputNotNull", "$L", true);
        }
        annotations.add(manyToOneBuilder.build());
        annotations.add(AnnotationSpec.builder(JOIN_COLUMN)
                .addMember("name", "$S", joinColumnName.toLowerCase())
                .addMember("foreignKeyType", "$T.FAKE", FOREIGN_KEY_TYPE)
                .build());
        return annotations;
    }

    public static List<AnnotationSpec> oneToMany(String mappedBy) {
        List<AnnotationSpec> annotations = new ArrayList<>();
        annotations.add(AnnotationSpec.builder(ONE_TO_MANY)
                .addMember("mappedBy", "$S", mappedBy)
                .build());
        return annotations;
    }

    public static List<AnnotationSpec> oneToOneOwning(String joinColumnName, boolean nullable) {
        List<AnnotationSpec> annotations = new ArrayList<>();
        // FAKE foreign key requires the property to always be @Nullable;
        // use inputNotNull=true to enforce non-null at save time when DB column is NOT NULL.
        annotations.add(nullable());
        AnnotationSpec.Builder oneToOneBuilder = AnnotationSpec.builder(ONE_TO_ONE);
        if (!nullable) {
            oneToOneBuilder.addMember("inputNotNull", "$L", true);
        }
        annotations.add(oneToOneBuilder.build());
        annotations.add(AnnotationSpec.builder(JOIN_COLUMN)
                .addMember("name", "$S", joinColumnName.toLowerCase())
                .addMember("foreignKeyType", "$T.FAKE", FOREIGN_KEY_TYPE)
                .build());
        return annotations;
    }

    public static List<AnnotationSpec> oneToOneInverse(String mappedBy, boolean nullable) {
        List<AnnotationSpec> annotations = new ArrayList<>();
        if (nullable) {
            annotations.add(nullable());
        }
        annotations.add(AnnotationSpec.builder(ONE_TO_ONE)
                .addMember("mappedBy", "$S", mappedBy)
                .build());
        return annotations;
    }

    public static List<AnnotationSpec> manyToManyOwning(String joinTableName,
                                                         String joinColumnName,
                                                         String inverseJoinColumnName) {
        List<AnnotationSpec> annotations = new ArrayList<>();
        annotations.add(AnnotationSpec.builder(MANY_TO_MANY).build());
        annotations.add(AnnotationSpec.builder(JOIN_TABLE)
                .addMember("name", "$S", joinTableName.toLowerCase())
                .addMember("joinColumnName", "$S", joinColumnName.toLowerCase())
                .addMember("inverseJoinColumnName", "$S", inverseJoinColumnName.toLowerCase())
                .build());
        return annotations;
    }

    public static List<AnnotationSpec> manyToManyInverse(String mappedBy) {
        List<AnnotationSpec> annotations = new ArrayList<>();
        annotations.add(AnnotationSpec.builder(MANY_TO_MANY)
                .addMember("mappedBy", "$S", mappedBy)
                .build());
        return annotations;
    }

    public static AnnotationSpec nullable() {
        return AnnotationSpec.builder(NULLABLE).build();
    }
}
