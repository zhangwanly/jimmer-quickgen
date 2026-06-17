package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.JoinTableConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JoinTableDetectorTest {

    private final JoinTableDetector detector = new JoinTableDetector();

    @Test
    void detectsStandardJoinTable() {
        TableModel joinTable = new TableModel("book_author_mapping", List.of(
                new ColumnModel("book_id", "BIGINT", false, false, 0),
                new ColumnModel("author_id", "BIGINT", false, false, 0)
        ), Set.of("book_id", "author_id"));

        Set<String> allTableNames = Set.of("book", "author", "book_store", "book_author_mapping");

        assertTrue(detector.isJoinTable(joinTable, allTableNames));
    }

    @Test
    void nonJoinTable_hasExtraColumns() {
        TableModel table = new TableModel("order_item", List.of(
                new ColumnModel("order_id", "BIGINT", false, false, 0),
                new ColumnModel("product_id", "BIGINT", false, false, 0),
                new ColumnModel("quantity", "INTEGER", false, false, 0)
        ), Set.of("order_id", "product_id"));

        Set<String> allTableNames = Set.of("order", "product", "order_item");

        assertFalse(detector.isJoinTable(table, allTableNames));
    }

    @Test
    void nonJoinTable_singlePK() {
        TableModel table = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("store_id", "BIGINT", true, false, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        Set<String> allTableNames = Set.of("book", "book_store");

        assertFalse(detector.isJoinTable(table, allTableNames));
    }

    @Test
    void nonJoinTable_fkReferencesNonexistentTable() {
        TableModel table = new TableModel("mapping", List.of(
                new ColumnModel("foo_id", "BIGINT", false, false, 0),
                new ColumnModel("bar_id", "BIGINT", false, false, 0)
        ), Set.of("foo_id", "bar_id"));

        Set<String> allTableNames = Set.of("mapping"); // foo and bar tables don't exist

        assertFalse(detector.isJoinTable(table, allTableNames));
    }

    @Test
    void detect_returnsAllJoinTables() {
        TableModel bookStore = new TableModel("book_store", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0)
        ), Set.of("id"));

        TableModel joinTable = new TableModel("book_author_mapping", List.of(
                new ColumnModel("book_id", "BIGINT", false, false, 0),
                new ColumnModel("author_id", "BIGINT", false, false, 0)
        ), Set.of("book_id", "author_id"));

        Set<String> allTableNames = Set.of("store", "book", "author", "book_author_mapping");

        Set<String> result = detector.detect(List.of(bookStore, joinTable), allTableNames);
        assertEquals(1, result.size());
        assertTrue(result.contains("book_author_mapping"));
    }

    @Test
    void getReferencedTables_returnsSorted() {
        TableModel joinTable = new TableModel("book_author_mapping", List.of(
                new ColumnModel("book_id", "BIGINT", false, false, 0),
                new ColumnModel("author_id", "BIGINT", false, false, 0)
        ), Set.of("book_id", "author_id"));

        Set<String> allTableNames = Set.of("book", "author");

        List<String> refs = detector.getReferencedTables(joinTable, allTableNames);
        assertEquals(2, refs.size());
        assertEquals("author", refs.get(0)); // alphabetically first
        assertEquals("book", refs.get(1));
    }

    @Test
    void getJoinColumnForRef() {
        TableModel joinTable = new TableModel("book_author_mapping", List.of(
                new ColumnModel("book_id", "BIGINT", false, false, 0),
                new ColumnModel("author_id", "BIGINT", false, false, 0)
        ), Set.of("book_id", "author_id"));

        assertEquals("book_id", detector.getJoinColumnForRef(joinTable, "book"));
        assertEquals("author_id", detector.getJoinColumnForRef(joinTable, "author"));
    }

    @Test
    void allowExtraNonFkColumns_true_acceptsTableWithExtraColumns() {
        JoinTableDetector lenientDetector = new JoinTableDetector(
                new JoinTableConfig(2, true));

        TableModel table = new TableModel("order_item", List.of(
                new ColumnModel("order_id", "BIGINT", false, false, 0),
                new ColumnModel("product_id", "BIGINT", false, false, 0),
                new ColumnModel("quantity", "INTEGER", false, false, 0)
        ), Set.of("order_id", "product_id"));

        Set<String> allTableNames = Set.of("order", "product", "order_item");

        assertTrue(lenientDetector.isJoinTable(table, allTableNames));
    }

    @Test
    void requiredForeignKeyCount_3_rejectsTableWithOnly2Refs() {
        JoinTableDetector strictDetector = new JoinTableDetector(
                new JoinTableConfig(3, false));

        TableModel joinTable = new TableModel("book_author_mapping", List.of(
                new ColumnModel("book_id", "BIGINT", false, false, 0),
                new ColumnModel("author_id", "BIGINT", false, false, 0)
        ), Set.of("book_id", "author_id"));

        Set<String> allTableNames = Set.of("book", "author", "book_author_mapping");

        assertFalse(strictDetector.isJoinTable(joinTable, allTableNames));
    }
}
