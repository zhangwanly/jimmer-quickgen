package io.github.zhangwanly.jimmer.quickgen.config;

import io.github.zhangwanly.jimmer.quickgen.analysis.TableRefOverride;
import org.babyfish.jimmer.sql.GenerationType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Unified configuration for the Jimmer QuickGen reverse engineering process.
 */
public final class QuickGenConfig {

    private final String basePackage;
    private final Path outputDir;
    private final GenerationType generationType;
    private final BaseEntityConfig baseEntityConfig;
    private final TypeMappingRegistry typeMappingRegistry;
    private final List<String> selfRefPatterns;
    private final JoinTableConfig joinTableConfig;
    private final SchemaValidatorConfig schemaValidatorConfig;
    private final List<TableRefOverride> tableRefOverrides;

    private QuickGenConfig(Builder builder) {
        this.basePackage = builder.basePackage;
        this.outputDir = builder.outputDir;
        this.generationType = builder.generationType;
        this.baseEntityConfig = builder.baseEntityConfig;
        this.typeMappingRegistry = builder.typeMappingRegistry;
        this.selfRefPatterns = List.copyOf(builder.selfRefPatterns);
        this.joinTableConfig = builder.joinTableConfig;
        this.schemaValidatorConfig = builder.schemaValidatorConfig;
        this.tableRefOverrides = List.copyOf(builder.tableRefOverrides);
    }

    public String basePackage() { return basePackage; }
    public Path outputDir() { return outputDir; }
    public GenerationType generationType() { return generationType; }
    public BaseEntityConfig baseEntityConfig() { return baseEntityConfig; }
    public TypeMappingRegistry typeMappingRegistry() { return typeMappingRegistry; }
    public List<String> selfRefPatterns() { return selfRefPatterns; }
    public JoinTableConfig joinTableConfig() { return joinTableConfig; }
    public SchemaValidatorConfig schemaValidatorConfig() { return schemaValidatorConfig; }
    public List<TableRefOverride> tableRefOverrides() { return tableRefOverrides; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String basePackage = "com.example.entity";
        private Path outputDir = Path.of("generated");
        private GenerationType generationType = GenerationType.USER;
        private BaseEntityConfig baseEntityConfig = BaseEntityConfig.defaults();
        private TypeMappingRegistry typeMappingRegistry = TypeMappingRegistry.defaults();
        private List<String> selfRefPatterns = List.of("parent_id", "root_id");
        private JoinTableConfig joinTableConfig = JoinTableConfig.defaults();
        private SchemaValidatorConfig schemaValidatorConfig = SchemaValidatorConfig.defaults();
        private final List<TableRefOverride> tableRefOverrides = new ArrayList<>();

        public Builder basePackage(String basePackage) {
            this.basePackage = basePackage;
            return this;
        }

        public Builder outputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Builder generationType(GenerationType generationType) {
            this.generationType = generationType;
            return this;
        }

        public Builder baseEntityConfig(BaseEntityConfig baseEntityConfig) {
            this.baseEntityConfig = baseEntityConfig;
            return this;
        }

        public Builder baseEntityConfig(Consumer<BaseEntityConfig.Builder> block) {
            BaseEntityConfig.Builder b = BaseEntityConfig.builder();
            block.accept(b);
            this.baseEntityConfig = b.build();
            return this;
        }

        public Builder typeMappingRegistry(TypeMappingRegistry typeMappingRegistry) {
            this.typeMappingRegistry = typeMappingRegistry;
            return this;
        }

        public Builder selfRefPatterns(List<String> selfRefPatterns) {
            this.selfRefPatterns = selfRefPatterns;
            return this;
        }

        public Builder joinTableConfig(JoinTableConfig joinTableConfig) {
            this.joinTableConfig = joinTableConfig;
            return this;
        }

        public Builder schemaValidatorConfig(SchemaValidatorConfig schemaValidatorConfig) {
            this.schemaValidatorConfig = schemaValidatorConfig;
            return this;
        }

        public Builder schemaValidatorConfig(Consumer<SchemaValidatorConfig.Builder> block) {
            SchemaValidatorConfig.Builder b = SchemaValidatorConfig.builder();
            block.accept(b);
            this.schemaValidatorConfig = b.build();
            return this;
        }

        /**
         * Add a table reference override for a legacy FK column.
         * <p>Example: {@code tableRefOverride("order_info", "user_id", "user_info")}
         * tells the resolver that {@code order_info.user_id} references {@code user_info} table.</p>
         *
         * @param tableName      the table owning the FK column
         * @param columnName     the FK column name
         * @param actualRefTable the actual referenced table name
         */
        public Builder tableRefOverride(String tableName, String columnName, String actualRefTable) {
            this.tableRefOverrides.add(new TableRefOverride(tableName, columnName, actualRefTable));
            return this;
        }

        /**
         * Replace all table reference overrides with the given list.
         */
        public Builder tableRefOverrides(List<TableRefOverride> overrides) {
            this.tableRefOverrides.clear();
            this.tableRefOverrides.addAll(overrides);
            return this;
        }

        public QuickGenConfig build() {
            return new QuickGenConfig(this);
        }
    }
}
