package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.config.SchemaValidatorConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {

    @Test
    void unresolvedIdColumn_generatesWarning() {
        // "store" table does not exist, but book has store_id
        TableModel book = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("store_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder().build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(book),
                Set.of(), // no base columns
                Map.of(), // no resolved associations
                Set.of("book")); // only book table exists

        assertEquals(1, warnings.size());
        SchemaWarning w = warnings.get(0);
        assertEquals("book", w.tableName());
        assertEquals("store_id", w.columnName());
        assertEquals("store", w.extractedRef());
        assertEquals(SchemaWarning.WarningType.UNRESOLVED_FK, w.type());
    }

    @Test
    void resolvedIdColumn_noWarning() {
        // store_id is resolved via ManyToOne association
        TableModel store = new TableModel("store", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel book = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("store_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        // Simulate resolved association for book.store_id
        Map<String, List<AssociationModel>> associationMap = new HashMap<>();
        associationMap.put("book", List.of(
                new AssociationModel.ManyToOneAssoc("store", "Store", true, "store_id")));

        QuickGenConfig config = QuickGenConfig.builder().build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(store, book),
                Set.of(),
                associationMap,
                Set.of("store", "book"));

        assertTrue(warnings.isEmpty(), "Resolved FK column should not produce warning");
    }

    @Test
    void whitelistedColumn_noWarning() {
        // open_id is in the default whitelist
        TableModel user = new TableModel("user", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("open_id", "VARCHAR", false, false, 100),
                new ColumnModel("union_id", "VARCHAR", true, false, 100)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder().build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(user),
                Set.of(),
                Map.of(),
                Set.of("user"));

        assertTrue(warnings.isEmpty(), "Whitelisted columns should not produce warnings");
    }

    @Test
    void customWhitelist_suppressesAdditionalWarnings() {
        // custom_id is not in default whitelist, add it via custom config
        TableModel order = new TableModel("order_tbl", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("custom_id", "VARCHAR", false, false, 50)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder()
                .schemaValidatorConfig(b -> b.addNonFkIdColumns("custom_id"))
                .build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(order),
                Set.of(),
                Map.of(),
                Set.of("order_tbl"));

        assertTrue(warnings.isEmpty(), "Custom whitelisted column should not produce warning");
    }

    @Test
    void baseColumn_noWarning() {
        // id is a base column → should be skipped
        TableModel book = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder().build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(book),
                Set.of("id"), // id is a base column
                Map.of(),
                Set.of("book"));

        assertTrue(warnings.isEmpty(), "Base columns should be skipped");
    }

    @Test
    void nonIdColumn_noWarning() {
        // Regular columns without _id suffix should not trigger warnings
        TableModel book = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100),
                new ColumnModel("edition", "INT", false, false, 0)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder().build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(book),
                Set.of(),
                Map.of(),
                Set.of("book"));

        assertTrue(warnings.isEmpty(), "Non-_id columns should not produce warnings");
    }

    @Test
    void multiLevelFkResolved_noWarning() {
        // category1_id, category2_id, category3_id all resolved via associations
        TableModel category = new TableModel("category", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel product = new TableModel("product", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("category1_id", "BIGINT", true, false, 0),
                new ColumnModel("category2_id", "BIGINT", true, false, 0),
                new ColumnModel("category3_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        // Simulate resolved associations for all 3 digit suffix columns
        Map<String, List<AssociationModel>> associationMap = new HashMap<>();
        associationMap.put("product", List.of(
                new AssociationModel.ManyToOneAssoc("category1", "Category", true, "category1_id"),
                new AssociationModel.ManyToOneAssoc("category2", "Category", true, "category2_id"),
                new AssociationModel.ManyToOneAssoc("category3", "Category", true, "category3_id")));

        QuickGenConfig config = QuickGenConfig.builder().build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(category, product),
                Set.of(),
                associationMap,
                Set.of("category", "product"));

        assertTrue(warnings.isEmpty(), "Resolved multi-level FK columns should not produce warnings");
    }

    @Test
    void loneDigitSuffixColumn_generatesWarning() {
        // Only category1_id exists (no category2_id, category3_id) — resolver requires >= 2
        TableModel category = new TableModel("category", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel product = new TableModel("product", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("category1_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder().build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(category, product),
                Set.of(),
                Map.of(), // no resolved associations
                Set.of("category", "product"));

        assertEquals(1, warnings.size());
        SchemaWarning w = warnings.get(0);
        assertEquals("product", w.tableName());
        assertEquals("category1_id", w.columnName());
        assertEquals("category", w.extractedRef());
        assertEquals(SchemaWarning.WarningType.INCOMPLETE_MULTI_LEVEL, w.type());
    }

    @Test
    void multipleWarnings_allReported() {
        // Multiple unresolved _id columns across tables
        TableModel orderTbl = new TableModel("order_tbl", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("warehouse_id", "BIGINT", true, false, 0),
                new ColumnModel("carrier_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder().build();
        SchemaValidator validator = new SchemaValidator(config);

        List<SchemaWarning> warnings = validator.validate(
                List.of(orderTbl),
                Set.of(),
                Map.of(),
                Set.of("order_tbl"));

        assertEquals(2, warnings.size());
        Set<String> columnNames = new HashSet<>();
        for (SchemaWarning w : warnings) {
            columnNames.add(w.columnName());
        }
        assertTrue(columnNames.contains("warehouse_id"));
        assertTrue(columnNames.contains("carrier_id"));
    }
}
