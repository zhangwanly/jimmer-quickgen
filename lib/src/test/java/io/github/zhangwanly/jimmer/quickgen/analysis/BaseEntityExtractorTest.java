package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.BaseEntityConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityExtractorTest {

    private final BaseEntityExtractor extractor = new BaseEntityExtractor();

    private final ColumnModel idCol = new ColumnModel("id", "BIGINT", false, true, 0);
    private final ColumnModel nameCol = new ColumnModel("name", "VARCHAR", false, false, 100);
    private final ColumnModel createdAtCol = new ColumnModel("created_at", "TIMESTAMP", true, false, 0);
    private final ColumnModel updatedAtCol = new ColumnModel("updated_at", "TIMESTAMP", true, false, 0);

    private TableModel table(String name, ColumnModel... cols) {
        return new TableModel(name, List.of(cols), Set.of("id"));
    }

    @Test
    void extractsMatchingColumns() {
        List<TableModel> tables = List.of(
                table("book_store", idCol, nameCol, createdAtCol, updatedAtCol)
        );
        BaseEntityConfig config = BaseEntityConfig.defaults(); // patterns: id, created_at, updated_at

        BaseEntityExtractionResult result = extractor.extract(tables, config);

        assertEquals(3, result.baseColumns().size());
        assertTrue(result.hasBaseEntity());

        Set<String> entityCols = result.getColumnNamesForTable("book_store");
        assertEquals(1, entityCols.size());
        assertTrue(entityCols.contains("name"));
    }

    @Test
    void disabledConfig_returnsAllColumnsAsEntityColumns() {
        List<TableModel> tables = List.of(
                table("book_store", idCol, nameCol, createdAtCol)
        );
        BaseEntityConfig config = BaseEntityConfig.builder().enabled(false).build();

        BaseEntityExtractionResult result = extractor.extract(tables, config);

        assertFalse(result.hasBaseEntity());
        assertTrue(result.baseColumns().isEmpty());
        assertEquals(3, result.getColumnNamesForTable("book_store").size());
    }

    @Test
    void customPatterns() {
        List<TableModel> tables = List.of(
                table("book_store", idCol, nameCol, createdAtCol,
                        new ColumnModel("tenant_id", "BIGINT", false, false, 0))
        );
        BaseEntityConfig config = BaseEntityConfig.builder()
                .columnPatterns("id", "tenant_id")
                .build();

        BaseEntityExtractionResult result = extractor.extract(tables, config);

        assertEquals(2, result.baseColumns().size());
        Set<String> entityCols = result.getColumnNamesForTable("book_store");
        assertEquals(2, entityCols.size());
        assertTrue(entityCols.contains("name"));
        assertTrue(entityCols.contains("created_at"));
    }

    @Test
    void regexPatterns() {
        List<TableModel> tables = List.of(
                table("book", idCol,
                        new ColumnModel("created_at", "TIMESTAMP", true, false, 0),
                        new ColumnModel("updated_at", "TIMESTAMP", true, false, 0),
                        nameCol)
        );
        BaseEntityConfig config = BaseEntityConfig.builder()
                .columnPatterns("id", "^.*_at$")
                .build();

        BaseEntityExtractionResult result = extractor.extract(tables, config);

        assertEquals(3, result.baseColumns().size()); // id, created_at, updated_at
    }

    @Test
    void multipleTables_deduplicatesBaseColumns() {
        List<TableModel> tables = List.of(
                table("book_store", idCol, nameCol, createdAtCol),
                table("author", idCol,
                        new ColumnModel("first_name", "VARCHAR", false, false, 50),
                        createdAtCol)
        );
        BaseEntityConfig config = BaseEntityConfig.defaults();

        BaseEntityExtractionResult result = extractor.extract(tables, config);

        // id and created_at should appear once each (deduplicated)
        assertEquals(2, result.baseColumns().size());
    }

    @Test
    void tableWithoutBaseColumns_stillWorks() {
        List<TableModel> tables = List.of(
                table("simple", new ColumnModel("code", "VARCHAR", false, false, 10),
                        new ColumnModel("value", "VARCHAR", false, false, 200))
        );
        BaseEntityConfig config = BaseEntityConfig.defaults();

        BaseEntityExtractionResult result = extractor.extract(tables, config);

        // No base columns matched in this table
        Set<String> entityCols = result.getColumnNamesForTable("simple");
        assertEquals(2, entityCols.size());
    }
}
