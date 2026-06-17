package io.github.zhangwanly.jimmer.quickgen.codegen;

import com.squareup.javapoet.AnnotationSpec;
import org.babyfish.jimmer.sql.GenerationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationFactoryTest {

    @Test
    void generatedValue_defaultIsUser() {
        AnnotationSpec spec = AnnotationFactory.generatedValue(GenerationType.USER);
        String rendered = spec.toString();
        assertTrue(rendered.contains("USER"), "Default generation type should be USER");
        assertTrue(rendered.contains("GeneratedValue"));
    }

    @Test
    void generatedValue_identity() {
        AnnotationSpec spec = AnnotationFactory.generatedValue(GenerationType.IDENTITY);
        String rendered = spec.toString();
        assertTrue(rendered.contains("IDENTITY"));
    }

    @Test
    void entity() {
        AnnotationSpec spec = AnnotationFactory.entity();
        assertTrue(spec.toString().contains("Entity"));
    }

    @Test
    void mappedSuperclass() {
        AnnotationSpec spec = AnnotationFactory.mappedSuperclass();
        assertTrue(spec.toString().contains("MappedSuperclass"));
    }

    @Test
    void columnWithName() {
        AnnotationSpec spec = AnnotationFactory.column("my_column");
        assertTrue(spec.toString().contains("my_column"));
    }

    @Test
    void manyToOne_hasForeignKeyTypeFake() {
        List<AnnotationSpec> annotations = AnnotationFactory.manyToOne("store_id", true);
        assertEquals(3, annotations.size()); // @Nullable, @ManyToOne, @JoinColumn

        String joinColumn = annotations.get(2).toString();
        assertTrue(joinColumn.contains("JoinColumn"));
        assertTrue(joinColumn.contains("store_id"));
        assertTrue(joinColumn.contains("FAKE"), "@JoinColumn must have foreignKeyType = FAKE");
    }

    @Test
    void manyToOne_notNullable_noNullableAnnotation() {
        List<AnnotationSpec> annotations = AnnotationFactory.manyToOne("store_id", false);
        assertEquals(2, annotations.size()); // @ManyToOne, @JoinColumn (no @Nullable)
    }

    @Test
    void oneToMany_hasMappedBy() {
        List<AnnotationSpec> annotations = AnnotationFactory.oneToMany("store");
        assertEquals(1, annotations.size());
        assertTrue(annotations.get(0).toString().contains("mappedBy"));
        assertTrue(annotations.get(0).toString().contains("store"));
    }

    @Test
    void oneToOneOwning_hasJoinColumnAndFakeFk() {
        List<AnnotationSpec> annotations = AnnotationFactory.oneToOneOwning("user_id", false);
        assertEquals(2, annotations.size()); // @OneToOne, @JoinColumn

        String joinColumn = annotations.get(1).toString();
        assertTrue(joinColumn.contains("user_id"));
        assertTrue(joinColumn.contains("FAKE"));
    }

    @Test
    void oneToOneInverse_hasMappedBy() {
        List<AnnotationSpec> annotations = AnnotationFactory.oneToOneInverse("address", true);
        assertEquals(2, annotations.size()); // @Nullable, @OneToOne(mappedBy=...)

        assertTrue(annotations.get(1).toString().contains("mappedBy"));
        assertTrue(annotations.get(1).toString().contains("address"));
    }

    @Test
    void manyToManyOwning_hasJoinTable() {
        List<AnnotationSpec> annotations = AnnotationFactory.manyToManyOwning(
                "book_author_mapping", "book_id", "author_id");
        assertEquals(2, annotations.size()); // @ManyToMany, @JoinTable

        String joinTable = annotations.get(1).toString();
        assertTrue(joinTable.contains("book_author_mapping"));
        assertTrue(joinTable.contains("book_id"));
        assertTrue(joinTable.contains("author_id"));
    }

    @Test
    void manyToManyInverse_hasMappedBy() {
        List<AnnotationSpec> annotations = AnnotationFactory.manyToManyInverse("books");
        assertEquals(1, annotations.size());
        assertTrue(annotations.get(0).toString().contains("mappedBy"));
        assertTrue(annotations.get(0).toString().contains("books"));
    }
}
