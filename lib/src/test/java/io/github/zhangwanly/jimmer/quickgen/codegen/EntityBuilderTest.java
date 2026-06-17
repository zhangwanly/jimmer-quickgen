package io.github.zhangwanly.jimmer.quickgen.codegen;

import io.github.zhangwanly.jimmer.quickgen.analysis.EntityModel;
import io.github.zhangwanly.jimmer.quickgen.analysis.PropertyModel;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.babyfish.jimmer.sql.GenerationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityBuilderTest {

    @Test
    void buildsBasicEntity() {
        TableModel table = new TableModel("book_store", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100),
                new ColumnModel("website", "VARCHAR", true, false, 255)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder()
                .basePackage("com.example.entity")
                .build();

        TypeSpec typeSpec = EntityBuilder.buildSimple(table, config);

        assertEquals("BookStore", typeSpec.name);
        assertTrue(typeSpec.kind == TypeSpec.Kind.INTERFACE);
        assertTrue(typeSpec.annotations.stream()
                .anyMatch(a -> a.type.toString().contains("Entity")));

        // Check methods
        assertTrue(typeSpec.methodSpecs.stream()
                .anyMatch(m -> m.name.equals("id")));
        assertTrue(typeSpec.methodSpecs.stream()
                .anyMatch(m -> m.name.equals("name")));
        assertTrue(typeSpec.methodSpecs.stream()
                .anyMatch(m -> m.name.equals("website")));
    }

    @Test
    void idPropertyHasIdAndGeneratedValueAnnotations() {
        TableModel table = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0)
        ), Set.of("id"));

        // Default: GenerationType.USER
        QuickGenConfig config = QuickGenConfig.builder().build();
        TypeSpec typeSpec = EntityBuilder.buildSimple(table, config);

        var idMethod = typeSpec.methodSpecs.stream()
                .filter(m -> m.name.equals("id"))
                .findFirst().orElseThrow();

        assertTrue(idMethod.annotations.stream()
                .anyMatch(a -> a.type.toString().contains("Id")),
                "id should have @Id annotation");
        assertTrue(idMethod.annotations.stream()
                .anyMatch(a -> a.toString().contains("USER")),
                "id should have @GeneratedValue(strategy = GenerationType.USER) by default");
    }

    @Test
    void generationTypeCanBeOverridden() {
        TableModel table = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder()
                .generationType(GenerationType.IDENTITY)
                .build();
        TypeSpec typeSpec = EntityBuilder.buildSimple(table, config);

        var idMethod = typeSpec.methodSpecs.stream()
                .filter(m -> m.name.equals("id"))
                .findFirst().orElseThrow();

        assertTrue(idMethod.annotations.stream()
                .anyMatch(a -> a.toString().contains("IDENTITY")),
                "id should have @GeneratedValue(strategy = GenerationType.IDENTITY) when overridden");
    }

    @Test
    void nullableColumnGetsNullableAnnotation() {
        TableModel table = new TableModel("book_store", List.of(
                new ColumnModel("website", "VARCHAR", true, false, 255)
        ), Set.of());

        QuickGenConfig config = QuickGenConfig.builder().build();
        TypeSpec typeSpec = EntityBuilder.buildSimple(table, config);

        var method = typeSpec.methodSpecs.stream()
                .filter(m -> m.name.equals("website"))
                .findFirst().orElseThrow();

        assertTrue(method.annotations.stream()
                .anyMatch(a -> a.type.toString().contains("Nullable")),
                "Nullable column should have @Nullable annotation");
    }

    @Test
    void excludesBaseEntityColumns() {
        TableModel table = new TableModel("book_store", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100),
                new ColumnModel("created_at", "TIMESTAMP", true, false, 0)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder()
                .baseEntityConfig(b -> b
                        .enabled(true)
                        .columnPatterns("id", "created_at"))
                .build();

        // Build EntityModel with only the "name" scalar (id and created_at are in BaseEntity)
        EntityModel entityModel = new EntityModel("book_store", "BookStore",
                List.of(new PropertyModel("name", "name", TypeName.get(String.class), false, false)),
                List.of(),
                true);

        TypeSpec typeSpec = EntityBuilder.build(entityModel, table, Set.of("id", "created_at"), config);

        // Should only have "name" method
        assertEquals(1, typeSpec.methodSpecs.size());
        assertEquals("name", typeSpec.methodSpecs.get(0).name);

        // Should extend BaseEntity
        assertEquals(1, typeSpec.superinterfaces.size());
        assertTrue(typeSpec.superinterfaces.get(0).toString().contains("BaseEntity"));
    }
}
