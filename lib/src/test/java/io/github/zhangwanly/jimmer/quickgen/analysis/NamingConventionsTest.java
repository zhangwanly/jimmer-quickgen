package io.github.zhangwanly.jimmer.quickgen.analysis;

import org.junit.jupiter.api.Test;

import java.util.Optional;

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
}
