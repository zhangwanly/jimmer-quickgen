package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.analysis.NamingConventions.DigitSuffixRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NamingConventionsTest {

    @Test
    void tableToEntityName_singleWord() {
        assertEquals("Book", NamingConventions.tableToEntityName("book"));
        assertEquals("Author", NamingConventions.tableToEntityName("author"));
    }

    @Test
    void tableToEntityName_multipleWords() {
        assertEquals("BookStore", NamingConventions.tableToEntityName("book_store"));
        assertEquals("BookAuthorMapping", NamingConventions.tableToEntityName("book_author_mapping"));
    }

    @Test
    void columnToPropertyName_singleWord() {
        assertEquals("id", NamingConventions.columnToPropertyName("id"));
        assertEquals("name", NamingConventions.columnToPropertyName("name"));
    }

    @Test
    void columnToPropertyName_multipleWords() {
        assertEquals("firstName", NamingConventions.columnToPropertyName("first_name"));
        assertEquals("createdAt", NamingConventions.columnToPropertyName("created_at"));
        assertEquals("storeId", NamingConventions.columnToPropertyName("store_id"));
    }

    @Test
    void entityToTableName() {
        assertEquals("book_store", NamingConventions.entityToTableName("BookStore"));
        assertEquals("author", NamingConventions.entityToTableName("Author"));
        assertEquals("book_author_mapping", NamingConventions.entityToTableName("BookAuthorMapping"));
    }

    @Test
    void propertyNameToColumnName() {
        assertEquals("first_name", NamingConventions.propertyNameToColumnName("firstName"));
        assertEquals("book_store_id", NamingConventions.propertyNameToColumnName("bookStoreId"));
    }

    @Test
    void extractReferenceTableName_validFkColumn() {
        assertEquals(Optional.of("store"), NamingConventions.extractReferenceTableName("store_id"));
        assertEquals(Optional.of("book_store"), NamingConventions.extractReferenceTableName("book_store_id"));
        assertEquals(Optional.of("author"), NamingConventions.extractReferenceTableName("author_id"));
    }

    @Test
    void extractReferenceTableName_nonFkColumn() {
        assertEquals(Optional.empty(), NamingConventions.extractReferenceTableName("first_name"));
        assertEquals(Optional.empty(), NamingConventions.extractReferenceTableName("id"));
        assertEquals(Optional.empty(), NamingConventions.extractReferenceTableName("created_at"));
    }

    @Test
    void pluralize() {
        assertEquals("books", NamingConventions.pluralize("book"));
        assertEquals("authors", NamingConventions.pluralize("author"));
        assertEquals("bookStores", NamingConventions.pluralize("bookStore"));
    }

    @Test
    void selfRefInverseName_wellKnownMapping() {
        assertEquals("children", NamingConventions.selfRefInverseName("parent", "category"));
        assertEquals("descendants", NamingConventions.selfRefInverseName("root", "treeNode"));
    }

    @Test
    void selfRefInverseName_fallbackToPluralize() {
        assertEquals("treeNodes", NamingConventions.selfRefInverseName("someRef", "treeNode"));
        assertEquals("categories", NamingConventions.selfRefInverseName("unknown", "category"));
    }

    @Test
    void toListPropertyName() {
        assertEquals("bookList", NamingConventions.toListPropertyName("book"));
        assertEquals("productDetailsList", NamingConventions.toListPropertyName("productDetails"));
        assertEquals("productSkuList", NamingConventions.toListPropertyName("productSku"));
    }

    @Test
    void extractDigitSuffixRef_match() {
        Set<String> tables = Set.of("category", "product");
        assertEquals(Optional.of(new DigitSuffixRef("category", 1)),
                NamingConventions.extractDigitSuffixRef("category1_id", tables));
        assertEquals(Optional.of(new DigitSuffixRef("category", 2)),
                NamingConventions.extractDigitSuffixRef("category2_id", tables));
        assertEquals(Optional.of(new DigitSuffixRef("category", 3)),
                NamingConventions.extractDigitSuffixRef("category3_id", tables));
    }

    @Test
    void extractDigitSuffixRef_multiDigit() {
        Set<String> tables = Set.of("category");
        assertEquals(Optional.of(new DigitSuffixRef("category", 12)),
                NamingConventions.extractDigitSuffixRef("category12_id", tables));
    }

    @Test
    void extractDigitSuffixRef_noDigit() {
        Set<String> tables = Set.of("category");
        // category_id has no digit suffix - should not match
        assertEquals(Optional.empty(), NamingConventions.extractDigitSuffixRef("category_id", tables));
    }

    @Test
    void extractDigitSuffixRef_tableNotMatch() {
        Set<String> tables = Set.of("category");
        assertEquals(Optional.empty(), NamingConventions.extractDigitSuffixRef("unknown1_id", tables));
    }

    @Test
    void extractDigitSuffixRef_nonFkColumn() {
        Set<String> tables = Set.of("first");
        assertEquals(Optional.empty(), NamingConventions.extractDigitSuffixRef("first_name", tables));
    }

    @Test
    void resolveTableRefOverride_match_returnsActualRef() {
        List<TableRefOverride> overrides = List.of(
                new TableRefOverride("order_info", "user_id", "user_info"));

        Optional<String> result = NamingConventions.resolveTableRefOverride(
                "order_info", "user_id", overrides);

        assertTrue(result.isPresent());
        assertEquals("user_info", result.get());
    }

    @Test
    void resolveTableRefOverride_noMatch_returnsEmpty() {
        List<TableRefOverride> overrides = List.of(
                new TableRefOverride("order_info", "user_id", "user_info"));

        Optional<String> result = NamingConventions.resolveTableRefOverride(
                "order_info", "address_id", overrides);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveTableRefOverride_wrongTable_noMatch() {
        // Same column name but different table — should not match
        List<TableRefOverride> overrides = List.of(
                new TableRefOverride("order_info", "user_id", "user_info"));

        Optional<String> result = NamingConventions.resolveTableRefOverride(
                "audit_log", "user_id", overrides);

        assertTrue(result.isEmpty(), "Override should only match the specified table");
    }

    @Test
    void resolveTableRefOverride_caseInsensitive() {
        List<TableRefOverride> overrides = List.of(
                new TableRefOverride("ORDER_INFO", "USER_ID", "USER_INFO"));

        Optional<String> result = NamingConventions.resolveTableRefOverride(
                "order_info", "user_id", overrides);

        assertTrue(result.isPresent());
        assertEquals("user_info", result.get());
    }

    @Test
    void resolveTableRefOverride_emptyOverrides() {
        Optional<String> result = NamingConventions.resolveTableRefOverride(
                "order_info", "user_id", List.of());

        assertTrue(result.isEmpty());
    }
}
