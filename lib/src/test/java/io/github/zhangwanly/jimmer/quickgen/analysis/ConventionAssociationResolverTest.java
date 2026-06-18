package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.analysis.AssociationModel.*;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConventionAssociationResolverTest {

    private final ConventionAssociationResolver resolver = new ConventionAssociationResolver();

    private QuickGenConfig defaultConfig() {
        return QuickGenConfig.builder().build();
    }

    @Test
    void detectsManyToOne() {
        TableModel store = new TableModel("store", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel book = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100),
                new ColumnModel("store_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(store, book), Set.of(), defaultConfig());

        // Book should have @ManyToOne -> Store
        List<AssociationModel> bookAssocs = result.get("book");
        assertTrue(bookAssocs.stream().anyMatch(a -> a instanceof ManyToOneAssoc m
                && m.propertyName().equals("store")
                && m.targetEntityName().equals("Store")
                && m.joinColumnName().equals("store_id")
                && m.nullable()));

        // Store should have @OneToMany -> Book
        List<AssociationModel> storeAssocs = result.get("store");
        assertTrue(storeAssocs.stream().anyMatch(a -> a instanceof OneToManyAssoc o
                && o.propertyName().equals("bookList")
                && o.targetEntityName().equals("Book")
                && o.mappedBy().equals("store")));
    }

    @Test
    void detectsSelfReference() {
        TableModel treeNode = new TableModel("tree_node", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100),
                new ColumnModel("parent_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(treeNode), Set.of(), defaultConfig());

        List<AssociationModel> assocs = result.get("tree_node");

        // Should have @ManyToOne parent()
        assertTrue(assocs.stream().anyMatch(a -> a instanceof ManyToOneAssoc m
                && m.propertyName().equals("parent")
                && m.targetEntityName().equals("TreeNode")
                && m.joinColumnName().equals("parent_id")));

        // Should have @OneToMany children()
        assertTrue(assocs.stream().anyMatch(a -> a instanceof OneToManyAssoc o
                && o.propertyName().equals("children")
                && o.mappedBy().equals("parent")));
    }

    @Test
    void detectsOneToOneWhenFkIsSolePK() {
        TableModel user = new TableModel("user", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel userProfile = new TableModel("user_profile", List.of(
                new ColumnModel("user_id", "BIGINT", false, false, 0),
                new ColumnModel("bio", "VARCHAR", true, false, 500)
        ), Set.of("user_id"));

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(user, userProfile), Set.of(), defaultConfig());

        // UserProfile should have @OneToOne owning -> User
        List<AssociationModel> profileAssocs = result.get("user_profile");
        assertTrue(profileAssocs.stream().anyMatch(a -> a instanceof OneToOneOwningAssoc o
                && o.propertyName().equals("user")
                && o.targetEntityName().equals("User")
                && o.joinColumnName().equals("user_id")));

        // User should have @OneToOne inverse -> UserProfile
        List<AssociationModel> userAssocs = result.get("user");
        assertTrue(userAssocs.stream().anyMatch(a -> a instanceof OneToOneInverseAssoc o
                && o.targetEntityName().equals("UserProfile")
                && o.mappedBy().equals("user")));
    }

    @Test
    void skipsBaseEntityColumns() {
        TableModel store = new TableModel("store", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel book = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("store_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        // Mark "id" as a base entity column
        Set<String> baseColumnNames = Set.of("id");

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(store, book), baseColumnNames, defaultConfig());

        // store_id should still be detected (it's not a base column)
        List<AssociationModel> bookAssocs = result.get("book");
        assertTrue(bookAssocs.stream().anyMatch(a -> a instanceof ManyToOneAssoc m
                && m.propertyName().equals("store")));
    }

    @Test
    void ignoresNonMatchingColumn() {
        TableModel table = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100),
                new ColumnModel("store_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        // Only "book" table exists, no "store" table -> store_id should be ignored
        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(table), Set.of(), defaultConfig());

        List<AssociationModel> assocs = result.get("book");
        assertTrue(assocs.isEmpty(), "store_id should be ignored when store table doesn't exist");
    }

    @Test
    void nonNullableManyToOne() {
        TableModel store = new TableModel("store", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0)
        ), Set.of("id"));

        TableModel book = new TableModel("book", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("store_id", "BIGINT", false, false, 0) // NOT NULL
        ), Set.of("id"));

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(store, book), Set.of(), defaultConfig());

        ManyToOneAssoc manyToOne = result.get("book").stream()
                .filter(a -> a instanceof ManyToOneAssoc)
                .map(a -> (ManyToOneAssoc) a)
                .findFirst().orElseThrow();

        assertFalse(manyToOne.nullable(), "Non-nullable FK should produce non-nullable ManyToOne");
    }
}
