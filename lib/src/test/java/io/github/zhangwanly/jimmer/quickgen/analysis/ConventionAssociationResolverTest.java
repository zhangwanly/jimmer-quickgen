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

    @Test
    void detectsMultiLevelFkAssociation() {
        TableModel category = new TableModel("category", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel product = new TableModel("product", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100),
                new ColumnModel("category1_id", "BIGINT", true, false, 0),
                new ColumnModel("category2_id", "BIGINT", true, false, 0),
                new ColumnModel("category3_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(category, product), Set.of(), defaultConfig());

        // Product should have 3 @ManyToOne -> Category
        List<AssociationModel> productAssocs = result.get("product");
        assertEquals(3, productAssocs.stream().filter(a -> a instanceof ManyToOneAssoc).count());

        assertTrue(productAssocs.stream().anyMatch(a -> a instanceof ManyToOneAssoc m
                && m.propertyName().equals("category1")
                && m.targetEntityName().equals("Category")
                && m.joinColumnName().equals("category1_id")));

        assertTrue(productAssocs.stream().anyMatch(a -> a instanceof ManyToOneAssoc m
                && m.propertyName().equals("category2")
                && m.targetEntityName().equals("Category")
                && m.joinColumnName().equals("category2_id")));

        assertTrue(productAssocs.stream().anyMatch(a -> a instanceof ManyToOneAssoc m
                && m.propertyName().equals("category3")
                && m.targetEntityName().equals("Category")
                && m.joinColumnName().equals("category3_id")));

        // Category should have 1 @OneToMany -> Product (mappedBy = "category3", the leaf node)
        List<AssociationModel> categoryAssocs = result.get("category");
        assertEquals(1, categoryAssocs.stream().filter(a -> a instanceof OneToManyAssoc).count());

        assertTrue(categoryAssocs.stream().anyMatch(a -> a instanceof OneToManyAssoc o
                && o.propertyName().equals("productList")
                && o.targetEntityName().equals("Product")
                && o.mappedBy().equals("category3")));
    }

    @Test
    void singleDigitSuffixFk_noAssociation() {
        TableModel category = new TableModel("category", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel product = new TableModel("product", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100),
                new ColumnModel("category1_id", "BIGINT", true, false, 0) // Only 1 digit suffix column
        ), Set.of("id"));

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(category, product), Set.of(), defaultConfig());

        // Product should have NO ManyToOne for category1_id (needs >=2 columns)
        List<AssociationModel> productAssocs = result.get("product");
        assertTrue(productAssocs.stream().noneMatch(a -> a instanceof ManyToOneAssoc));

        // Category should have NO OneToMany
        List<AssociationModel> categoryAssocs = result.get("category");
        assertTrue(categoryAssocs.stream().noneMatch(a -> a instanceof OneToManyAssoc));
    }

    @Test
    void overrideResolvesLegacyFkColumn() {
        // order_info.user_id should reference user_info, not user
        TableModel userInfo = new TableModel("user_info", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel orderInfo = new TableModel("order_info", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("user_id", "BIGINT", true, false, 0),
                new ColumnModel("total", "DECIMAL", false, false, 10)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder()
                .tableRefOverride("order_info", "user_id", "user_info")
                .build();

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(userInfo, orderInfo), Set.of(), config);

        // order_info should have @ManyToOne -> UserInfo
        List<AssociationModel> orderAssocs = result.get("order_info");
        assertTrue(orderAssocs.stream().anyMatch(a -> a instanceof ManyToOneAssoc m
                && m.propertyName().equals("user")
                && m.targetEntityName().equals("UserInfo")
                && m.joinColumnName().equals("user_id")));

        // user_info should have @OneToMany -> OrderInfo
        List<AssociationModel> userAssocs = result.get("user_info");
        assertTrue(userAssocs.stream().anyMatch(a -> a instanceof OneToManyAssoc o
                && o.propertyName().equals("orderInfoList")
                && o.targetEntityName().equals("OrderInfo")
                && o.mappedBy().equals("user")));
    }

    @Test
    void override_noMatchingTable_skipped() {
        // Override points to user_info, but user_info table doesn't exist
        TableModel orderInfo = new TableModel("order_info", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("user_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder()
                .tableRefOverride("order_info", "user_id", "user_info")
                .build();

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(orderInfo), Set.of(), config);

        // No association should be created
        List<AssociationModel> orderAssocs = result.get("order_info");
        assertTrue(orderAssocs.isEmpty(), "Override to nonexistent table should create no association");
    }

    @Test
    void override_onlyAffectsSpecifiedTable() {
        // order_info.user_id has override, audit_log.user_id does not
        TableModel userInfo = new TableModel("user_info", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel orderInfo = new TableModel("order_info", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("user_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        TableModel auditLog = new TableModel("audit_log", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("user_id", "BIGINT", true, false, 0)
        ), Set.of("id"));

        QuickGenConfig config = QuickGenConfig.builder()
                .tableRefOverride("order_info", "user_id", "user_info")
                .build();

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(userInfo, orderInfo, auditLog), Set.of(), config);

        // order_info.user_id should be resolved
        List<AssociationModel> orderAssocs = result.get("order_info");
        assertTrue(orderAssocs.stream().anyMatch(a -> a instanceof ManyToOneAssoc m
                && m.joinColumnName().equals("user_id")),
                "order_info.user_id should be resolved via override");

        // audit_log.user_id should NOT be resolved (no override)
        List<AssociationModel> auditAssocs = result.get("audit_log");
        assertTrue(auditAssocs.stream().noneMatch(a -> a instanceof ManyToOneAssoc),
                "audit_log.user_id should not be resolved without override");
    }

    @Test
    void overrideResolvesOneToOneWhenFkIsSolePk() {
        TableModel userInfo = new TableModel("user_info", List.of(
                new ColumnModel("id", "BIGINT", false, true, 0),
                new ColumnModel("name", "VARCHAR", false, false, 100)
        ), Set.of("id"));

        TableModel userProfile = new TableModel("user_profile", List.of(
                new ColumnModel("user_id", "BIGINT", false, false, 0),
                new ColumnModel("bio", "VARCHAR", true, false, 500)
        ), Set.of("user_id"));

        QuickGenConfig config = QuickGenConfig.builder()
                .tableRefOverride("user_profile", "user_id", "user_info")
                .build();

        Map<String, List<AssociationModel>> result = resolver.resolve(
                List.of(userInfo, userProfile), Set.of(), config);

        // userProfile should have @OneToOne owning -> UserInfo
        List<AssociationModel> profileAssocs = result.get("user_profile");
        assertTrue(profileAssocs.stream().anyMatch(a -> a instanceof OneToOneOwningAssoc o
                && o.propertyName().equals("user")
                && o.targetEntityName().equals("UserInfo")
                && o.joinColumnName().equals("user_id")));

        // user_info should have @OneToOne inverse -> UserProfile
        List<AssociationModel> userAssocs = result.get("user_info");
        assertTrue(userAssocs.stream().anyMatch(a -> a instanceof OneToOneInverseAssoc o
                && o.targetEntityName().equals("UserProfile")
                && o.mappedBy().equals("user")));
    }
}
