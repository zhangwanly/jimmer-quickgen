package io.github.zhangwanly.jimmer.quickgen.codegen;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PropertyMethodBuilderTest {

    private static final QuickGenConfig CONFIG = QuickGenConfig.builder().build();

    @Test
    void booleanIsPrefixColumn_hasExplicitColumnAnnotation() {
        // is_deleted (TINYINT, not null) → boolean isDeleted() with @Column("is_deleted")
        ColumnModel column = new ColumnModel("is_deleted", "TINYINT", false, false, 0);
        MethodSpec method = PropertyMethodBuilder.build(column, false, CONFIG);

        assertEquals("isDeleted", method.name);
        assertEquals(TypeName.BOOLEAN, method.returnType);

        String rendered = method.toString();
        assertTrue(rendered.contains("is_deleted"),
                "boolean isXxx property must have explicit @Column because Jimmer strips 'is' prefix");
    }

    @Test
    void nullableBooleanIsPrefixColumn_hasExplicitColumnAnnotation() {
        // is_enabled (TINYINT, nullable) → @Nullable Boolean isEnabled() with @Column("is_enabled")
        ColumnModel column = new ColumnModel("is_enabled", "TINYINT", true, false, 0);
        MethodSpec method = PropertyMethodBuilder.build(column, false, CONFIG);

        assertEquals("isEnabled", method.name);
        assertEquals(TypeName.get(Boolean.class), method.returnType);

        String rendered = method.toString();
        assertTrue(rendered.contains("is_enabled"),
                "Boolean isXxx property must have explicit @Column because Jimmer strips 'is' prefix");
    }

    @Test
    void booleanIsPrefixViaBuildScalar_hasExplicitColumnAnnotation() {
        // Directly test buildScalar for boolean isDeleted property
        MethodSpec method = PropertyMethodBuilder.buildScalar(
                "isDeleted", "is_deleted", TypeName.BOOLEAN, false, false, CONFIG);

        assertEquals("isDeleted", method.name);
        String rendered = method.toString();
        assertTrue(rendered.contains("is_deleted"),
                "boolean isXxx property must have explicit @Column because Jimmer strips 'is' prefix");
    }

    @Test
    void abbreviationProperty_hasExplicitColumnAnnotation() {
        // Property with consecutive uppercase (abbreviation) → must have @Column
        MethodSpec method = PropertyMethodBuilder.buildScalar(
                "serverURL", "server_url", TypeName.get(String.class), false, false, CONFIG);

        String rendered = method.toString();
        assertTrue(rendered.contains("server_url"),
                "Property with consecutive uppercase must have explicit @Column to avoid ambiguity");
    }

    @Test
    void getPrefixProperty_hasExplicitColumnAnnotation() {
        // get_type → getType(), Jimmer strips "get" → column "type", must have @Column("get_type")
        MethodSpec method = PropertyMethodBuilder.buildScalar(
                "getType", "get_type", TypeName.get(Integer.class), false, false, CONFIG);

        String rendered = method.toString();
        assertTrue(rendered.contains("name = \"get_type\""),
                "get-prefixed property must have explicit @Column because Jimmer strips 'get' prefix");
    }

    @Test
    void setPrefixProperty_hasExplicitColumnAnnotation() {
        // set_name → setName(), Jimmer strips "set" → column "name", must have @Column("set_name")
        MethodSpec method = PropertyMethodBuilder.buildScalar(
                "setName", "set_name", TypeName.get(String.class), false, false, CONFIG);

        String rendered = method.toString();
        assertTrue(rendered.contains("name = \"set_name\""),
                "set-prefixed property must have explicit @Column because Jimmer strips 'set' prefix");
    }

    @Test
    void regularCamelCaseProperty_noColumnAnnotationWhenNamesMatch() {
        // user_name → userName, no ambiguity, no @Column needed
        MethodSpec method = PropertyMethodBuilder.buildScalar(
                "userName", "user_name", TypeName.get(String.class), false, false, CONFIG);

        String rendered = method.toString();
        assertFalse(rendered.contains("@Column"),
                "Regular camelCase property with matching column name should NOT have @Column");
    }

    @Test
    void nonBooleanProperty_noColumnAnnotationWhenNamesMatch() {
        // status (VARCHAR) → String status(), no @Column needed (names match exactly)
        ColumnModel column = new ColumnModel("status", "VARCHAR", false, false, 50);
        MethodSpec method = PropertyMethodBuilder.build(column, false, CONFIG);

        assertEquals("status", method.name);
        assertEquals(TypeName.get(String.class), method.returnType);

        String rendered = method.toString();
        assertFalse(rendered.contains("@Column"),
                "Regular property with matching column name should NOT have @Column");
    }
}
