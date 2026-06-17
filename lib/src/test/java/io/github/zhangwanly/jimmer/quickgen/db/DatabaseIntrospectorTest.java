package io.github.zhangwanly.jimmer.quickgen.db;

import io.github.zhangwanly.jimmer.quickgen.TestDataSourceHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseIntrospectorTest {

    private static final String DB_URL = "jdbc:h2:mem:introspect_test;DB_CLOSE_DELAY=-1";

    @BeforeAll
    static void setUp() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS book_store (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "website VARCHAR(255))");

            stmt.execute("CREATE TABLE IF NOT EXISTS author (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "first_name VARCHAR(50) NOT NULL, " +
                    "last_name VARCHAR(50) NOT NULL, " +
                    "gender VARCHAR(10))");

            stmt.execute("CREATE TABLE IF NOT EXISTS book (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "edition INT NOT NULL DEFAULT 1, " +
                    "price DECIMAL(10,2), " +
                    "store_id BIGINT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS book_author_mapping (" +
                    "book_id BIGINT NOT NULL, " +
                    "author_id BIGINT NOT NULL, " +
                    "PRIMARY KEY (book_id, author_id))");
        }
    }

    private DatabaseIntrospector createIntrospector() {
        return new DatabaseIntrospector(TestDataSourceHelper.create(DB_URL));
    }

    @Test
    void discoversAllTables() {
        List<TableModel> tables = createIntrospector().introspect();
        assertTrue(tables.size() >= 4);

        Set<String> tableNames = new java.util.HashSet<>();
        for (TableModel t : tables) {
            tableNames.add(t.tableName().toUpperCase());
        }
        assertTrue(tableNames.contains("BOOK_STORE"));
        assertTrue(tableNames.contains("AUTHOR"));
        assertTrue(tableNames.contains("BOOK"));
        assertTrue(tableNames.contains("BOOK_AUTHOR_MAPPING"));
    }

    @Test
    void discoversColumns() {
        List<TableModel> tables = createIntrospector().introspect();
        TableModel author = tables.stream()
                .filter(t -> t.tableName().equalsIgnoreCase("AUTHOR"))
                .findFirst().orElseThrow();

        assertEquals(4, author.columns().size());
        assertTrue(author.getColumn("FIRST_NAME").isPresent());
        assertTrue(author.getColumn("LAST_NAME").isPresent());
        assertTrue(author.getColumn("GENDER").isPresent());
        assertTrue(author.getColumn("ID").isPresent());
    }

    @Test
    void discoversPrimaryKeys() {
        List<TableModel> tables = createIntrospector().introspect();

        TableModel bookStore = tables.stream()
                .filter(t -> t.tableName().equalsIgnoreCase("BOOK_STORE"))
                .findFirst().orElseThrow();
        assertTrue(bookStore.isPrimaryKeyColumn("ID"));
        assertFalse(bookStore.hasCompositePrimaryKey());

        TableModel mapping = tables.stream()
                .filter(t -> t.tableName().equalsIgnoreCase("BOOK_AUTHOR_MAPPING"))
                .findFirst().orElseThrow();
        assertTrue(mapping.hasCompositePrimaryKey());
        assertEquals(2, mapping.primaryKeyColumns().size());
    }

    @Test
    void discoversNullableColumns() {
        List<TableModel> tables = createIntrospector().introspect();
        TableModel bookStore = tables.stream()
                .filter(t -> t.tableName().equalsIgnoreCase("BOOK_STORE"))
                .findFirst().orElseThrow();

        ColumnModel name = bookStore.getColumn("NAME").orElseThrow();
        assertFalse(name.nullable());

        ColumnModel website = bookStore.getColumn("WEBSITE").orElseThrow();
        assertTrue(website.nullable());
    }
}
