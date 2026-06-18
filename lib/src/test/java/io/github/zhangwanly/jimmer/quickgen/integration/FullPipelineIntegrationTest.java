package io.github.zhangwanly.jimmer.quickgen.integration;

import io.github.zhangwanly.jimmer.quickgen.QuickGen;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import com.squareup.javapoet.JavaFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.zhangwanly.jimmer.quickgen.TestDataSourceHelper;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FullPipelineIntegrationTest {

    private static final String DB_URL = "jdbc:h2:mem:full_pipeline;DB_CLOSE_DELAY=-1";
    private Path outputDir;

    @BeforeEach
    void setUp() throws Exception {
        outputDir = Files.createTempDirectory("quickgen-test-");

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS book_author_mapping");
            stmt.execute("DROP TABLE IF EXISTS book");
            stmt.execute("DROP TABLE IF EXISTS author");
            stmt.execute("DROP TABLE IF EXISTS store");
            stmt.execute("DROP TABLE IF EXISTS tree_node");

            stmt.execute("CREATE TABLE store (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "website VARCHAR(255), " +
                    "created_at TIMESTAMP, " +
                    "updated_at TIMESTAMP)");

            stmt.execute("CREATE TABLE author (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "first_name VARCHAR(50) NOT NULL, " +
                    "last_name VARCHAR(50) NOT NULL, " +
                    "created_at TIMESTAMP, " +
                    "updated_at TIMESTAMP)");

            stmt.execute("CREATE TABLE book (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "edition INT NOT NULL DEFAULT 1, " +
                    "price DECIMAL(10,2), " +
                    "store_id BIGINT, " +
                    "created_at TIMESTAMP, " +
                    "updated_at TIMESTAMP)");

            stmt.execute("CREATE TABLE book_author_mapping (" +
                    "book_id BIGINT NOT NULL, " +
                    "author_id BIGINT NOT NULL, " +
                    "PRIMARY KEY (book_id, author_id))");

            stmt.execute("CREATE TABLE tree_node (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "parent_id BIGINT)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (outputDir != null && Files.exists(outputDir)) {
            try (Stream<Path> paths = Files.walk(outputDir).sorted(Comparator.reverseOrder())) {
                paths.forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
        }
    }

    private javax.sql.DataSource createDataSource() {
        return TestDataSourceHelper.create(DB_URL);
    }

    @Test
    void fullPipeline_generatesAllEntities() throws Exception {
        QuickGenConfig config = QuickGenConfig.builder()
                .basePackage("com.example.entity")
                .outputDir(outputDir)
                .baseEntityConfig(b -> b
                        .enabled(true)
                        .name("BaseEntity")
                        .columnPatterns("id", "created_at", "updated_at"))
                .build();

        List<JavaFile> files = QuickGen.generate(createDataSource(), config);

        // 5 entities: BaseEntity + Store + Author + Book + TreeNode
        // book_author_mapping is a join table → no entity
        assertEquals(5, files.size());

        // Verify BaseEntity
        String baseEntity = readGeneratedFile("BaseEntity.java");
        assertNotNull(baseEntity);
        assertTrue(baseEntity.contains("@MappedSuperclass"));
        assertTrue(baseEntity.contains("long id()"));
        assertTrue(baseEntity.contains("@Id"));
        assertTrue(baseEntity.contains("GenerationType.USER"));
        assertTrue(baseEntity.contains("LocalDateTime createdAt()"));
        assertTrue(baseEntity.contains("LocalDateTime updatedAt()"));
        assertTrue(baseEntity.contains("@Nullable"));

        // Verify Store
        String store = readGeneratedFile("Store.java");
        assertNotNull(store);
        assertTrue(store.contains("@Entity"));
        assertTrue(store.contains("extends BaseEntity"));
        assertTrue(store.contains("String name()"));
        assertTrue(store.contains("@Nullable"));
        assertTrue(store.contains("String website()"));
        assertTrue(store.contains("@OneToMany"));
        assertTrue(store.contains("mappedBy = \"store\""));
        assertTrue(store.contains("List<Book> bookList()"));
        // Should NOT contain base entity columns
        assertFalse(store.contains("long id()"), "id should be in BaseEntity, not Store");
        assertFalse(store.contains("createdAt()"), "createdAt should be in BaseEntity");

        // Verify Book
        String book = readGeneratedFile("Book.java");
        assertNotNull(book);
        assertTrue(book.contains("@Entity"));
        assertTrue(book.contains("extends BaseEntity"));
        assertTrue(book.contains("String name()"));
        assertTrue(book.contains("int edition()"));
        assertTrue(book.contains("BigDecimal price()"));
        assertTrue(book.contains("@ManyToOne"));
        assertTrue(book.contains("Store store()"));
        assertTrue(book.contains("foreignKeyType = ForeignKeyType.FAKE"));
        // FK column store_id should NOT generate a separate scalar property
        assertFalse(book.contains("Long storeId()"), "FK column should not generate scalar when association exists");
        // Book is the inverse side of ManyToMany (author is owning side alphabetically)
        assertTrue(book.contains("@ManyToMany"));
        assertTrue(book.contains("mappedBy = \"books\""));
        assertTrue(book.contains("List<Author> authors()"));

        // Verify Author
        String author = readGeneratedFile("Author.java");
        assertNotNull(author);
        assertTrue(author.contains("@Entity"));
        assertTrue(author.contains("extends BaseEntity"));
        assertTrue(author.contains("String firstName()"));
        assertTrue(author.contains("String lastName()"));
        // Author is the owning side of ManyToMany (alphabetically first)
        assertTrue(author.contains("@ManyToMany"));
        assertTrue(author.contains("@JoinTable("));
        assertTrue(author.contains("name = \"book_author_mapping\""));
        assertTrue(author.contains("joinColumnName = \"author_id\""));
        assertTrue(author.contains("inverseJoinColumnName = \"book_id\""));
        assertTrue(author.contains("List<Book> books()"));

        // Verify TreeNode
        String treeNode = readGeneratedFile("TreeNode.java");
        assertNotNull(treeNode);
        assertTrue(treeNode.contains("@Entity"));
        assertTrue(treeNode.contains("extends BaseEntity"));
        assertTrue(treeNode.contains("String name()"));
        assertTrue(treeNode.contains("@ManyToOne"));
        assertTrue(treeNode.contains("TreeNode parent()"));
        assertTrue(treeNode.contains("@OneToMany"));
        assertTrue(treeNode.contains("mappedBy = \"parent\""));
        assertTrue(treeNode.contains("List<TreeNode> children()"));
        // FK column parent_id should NOT generate a separate scalar property
        assertFalse(treeNode.contains("Long parentId()"), "FK column should not generate scalar when association exists");

        // Verify BookAuthorMapping was NOT generated
        String mapping = readGeneratedFile("BookAuthorMapping.java");
        assertNull(mapping, "Join table should NOT generate an entity");
    }

    @Test
    void allJoinColumnsHaveFakeForeignKeyType() throws Exception {
        QuickGenConfig config = QuickGenConfig.builder()
                .basePackage("com.example.entity")
                .outputDir(outputDir)
                .build();

        QuickGen.generate(createDataSource(), config);

        // Check all generated files for @JoinColumn → must have FAKE
        Path entityDir = outputDir.resolve("com/example/entity");
        if (Files.exists(entityDir)) {
            try (Stream<Path> files = Files.list(entityDir)) {
                files.filter(p -> p.toString().endsWith(".java"))
                        .forEach(p -> {
                            try {
                                String content = Files.readString(p);
                                if (content.contains("@JoinColumn")) {
                                    assertTrue(content.contains("ForeignKeyType.FAKE"),
                                            p.getFileName() + " should have foreignKeyType = FAKE");
                                }
                            } catch (IOException e) {
                                fail("Failed to read " + p);
                            }
                        });
            }
        }
    }

    @Test
    void customGenerationType() throws Exception {
        QuickGenConfig config = QuickGenConfig.builder()
                .basePackage("com.example.entity")
                .outputDir(outputDir)
                .generationType(org.babyfish.jimmer.sql.GenerationType.IDENTITY)
                .build();

        QuickGen.generate(createDataSource(), config);

        String baseEntity = readGeneratedFile("BaseEntity.java");
        assertNotNull(baseEntity);
        assertTrue(baseEntity.contains("GenerationType.IDENTITY"));
    }

    @Test
    void multiLevelFk_generatesAssociations() throws Exception {
        // Setup additional tables for multi-level FK test
        String multiLevelDbUrl = "jdbc:h2:mem:multi_level_fk;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(multiLevelDbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE category (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL)");

            stmt.execute("CREATE TABLE product (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "category1_id BIGINT, " +
                    "category2_id BIGINT, " +
                    "category3_id BIGINT)");

            QuickGenConfig config = QuickGenConfig.builder()
                    .basePackage("com.example.entity")
                    .outputDir(outputDir)
                    .build();

            QuickGen.generate(TestDataSourceHelper.create(multiLevelDbUrl), config);
        }

        // Verify Product has 3 @ManyToOne associations
        String product = readGeneratedFile("Product.java");
        assertNotNull(product);
        assertTrue(product.contains("@ManyToOne"), "Product should have @ManyToOne");
        assertTrue(product.contains("Category category1()"), "Product should have category1()");
        assertTrue(product.contains("Category category2()"), "Product should have category2()");
        assertTrue(product.contains("Category category3()"), "Product should have category3()");

        // Product should NOT have scalar properties for category FK columns
        assertFalse(product.contains("Long category1Id()"), "category1_id should not generate scalar");
        assertFalse(product.contains("Long category2Id()"), "category2_id should not generate scalar");
        assertFalse(product.contains("Long category3Id()"), "category3_id should not generate scalar");

        // Verify Category has @OneToMany mappedBy to category3 (leaf node)
        String category = readGeneratedFile("Category.java");
        assertNotNull(category);
        assertTrue(category.contains("@OneToMany"), "Category should have @OneToMany");
        assertTrue(category.contains("mappedBy = \"category3\""), "OneToMany should map to category3 (leaf node)");
        assertTrue(category.contains("List<Product> productList()"), "Category should have productList()");
    }

    private String readGeneratedFile(String fileName) throws IOException {
        Path file = outputDir.resolve("com/example/entity/" + fileName);
        if (Files.exists(file)) {
            return Files.readString(file);
        }
        return null;
    }

    @Test
    void unresolvedIdColumn_generatesWarningsReport() throws Exception {
        // Setup DB with an unresolved _id column (warehouse table doesn't exist)
        String unresolvedDbUrl = "jdbc:h2:mem:unresolved_fk;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(unresolvedDbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE simple_order (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "warehouse_id BIGINT)"); // no "warehouse" table

            QuickGenConfig config = QuickGenConfig.builder()
                    .basePackage("com.example.entity")
                    .outputDir(outputDir)
                    .build();

            QuickGen.generate(TestDataSourceHelper.create(unresolvedDbUrl), config);
        }

        // Verify schema-warnings.md was written
        Path warningsFile = outputDir.resolve("schema-warnings.md");
        assertTrue(Files.exists(warningsFile), "schema-warnings.md should be generated");

        String content = Files.readString(warningsFile);
        assertTrue(content.contains("# Schema 验证报告"), "Should have Markdown header");
        assertTrue(content.contains("`warehouse_id`"), "Should mention unresolved column in code format");
        assertTrue(content.contains("`warehouse`"), "Should mention extracted table reference in code format");
        assertTrue(content.contains("引用表不存在"), "Should contain Chinese error description");
    }

    @Test
    void resolvedSchema_noWarningsReport() throws Exception {
        // Use the standard DB setup where all _id columns are resolved
        QuickGenConfig config = QuickGenConfig.builder()
                .basePackage("com.example.entity")
                .outputDir(outputDir)
                .build();

        QuickGen.generate(createDataSource(), config);

        // Verify schema-warnings.md was NOT written (all _id columns are resolved)
        Path warningsFile = outputDir.resolve("schema-warnings.md");
        assertFalse(Files.exists(warningsFile),
                "schema-warnings.md should not be generated when all _id columns are resolved");
    }

    @Test
    void tableRefOverride_generatesCorrectAssociations() throws Exception {
        // Setup: user_info and order_info tables
        // order_info.user_id should reference user_info (not "user" which doesn't exist)
        String overrideDbUrl = "jdbc:h2:mem:table_ref_override;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(overrideDbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE user_info (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL)");

            stmt.execute("CREATE TABLE order_info (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT, " +
                    "total DECIMAL(10,2) NOT NULL)");

            QuickGenConfig config = QuickGenConfig.builder()
                    .basePackage("com.example.entity")
                    .outputDir(outputDir)
                    .tableRefOverride("order_info", "user_id", "user_info")
                    .build();

            QuickGen.generate(TestDataSourceHelper.create(overrideDbUrl), config);
        }

        // Verify OrderInfo has @ManyToOne -> UserInfo
        String orderInfo = readGeneratedFile("OrderInfo.java");
        assertNotNull(orderInfo);
        assertTrue(orderInfo.contains("@ManyToOne"), "OrderInfo should have @ManyToOne");
        assertTrue(orderInfo.contains("UserInfo user()"), "OrderInfo should have user() property");
        // FK column should NOT generate a scalar property
        assertFalse(orderInfo.contains("Long userId()"), "user_id should not generate scalar when override resolves it");

        // Verify UserInfo has @OneToMany -> OrderInfo
        String userInfo = readGeneratedFile("UserInfo.java");
        assertNotNull(userInfo);
        assertTrue(userInfo.contains("@OneToMany"), "UserInfo should have @OneToMany");
        assertTrue(userInfo.contains("mappedBy = \"user\""), "OneToMany should map to user property");
        assertTrue(userInfo.contains("List<OrderInfo> orderInfoList()"), "UserInfo should have orderInfoList()");

        // Verify NO schema-warnings.md (all FK columns resolved via override)
        Path warningsFile = outputDir.resolve("schema-warnings.md");
        assertFalse(Files.exists(warningsFile),
                "schema-warnings.md should not be generated when tableRefOverride resolves all _id columns");
    }
}
