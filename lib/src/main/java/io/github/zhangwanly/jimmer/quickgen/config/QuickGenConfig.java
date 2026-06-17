package io.github.zhangwanly.jimmer.quickgen.config;

import org.babyfish.jimmer.sql.GenerationType;

import java.nio.file.Path;
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

    private QuickGenConfig(Builder builder) {
        this.basePackage = builder.basePackage;
        this.outputDir = builder.outputDir;
        this.generationType = builder.generationType;
        this.baseEntityConfig = builder.baseEntityConfig;
        this.typeMappingRegistry = builder.typeMappingRegistry;
        this.selfRefPatterns = List.copyOf(builder.selfRefPatterns);
        this.joinTableConfig = builder.joinTableConfig;
    }

    public String basePackage() { return basePackage; }
    public Path outputDir() { return outputDir; }
    public GenerationType generationType() { return generationType; }
    public BaseEntityConfig baseEntityConfig() { return baseEntityConfig; }
    public TypeMappingRegistry typeMappingRegistry() { return typeMappingRegistry; }
    public List<String> selfRefPatterns() { return selfRefPatterns; }
    public JoinTableConfig joinTableConfig() { return joinTableConfig; }

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

        public QuickGenConfig build() {
            return new QuickGenConfig(this);
        }
    }
}
