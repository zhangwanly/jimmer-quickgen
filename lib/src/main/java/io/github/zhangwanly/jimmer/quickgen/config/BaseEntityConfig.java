package io.github.zhangwanly.jimmer.quickgen.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for BaseEntity extraction.
 *
 * <p>When enabled, columns matching the configured patterns are extracted
 * from individual entities into a shared {@code @MappedSuperclass} interface.</p>
 */
public final class BaseEntityConfig {

    private final boolean enabled;
    private final String name;
    private final List<String> columnPatterns;

    private BaseEntityConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.name = builder.name;
        this.columnPatterns = List.copyOf(builder.columnPatterns);
    }

    public boolean enabled() { return enabled; }
    public String name() { return name; }
    public List<String> columnPatterns() { return columnPatterns; }

    /**
     * Check if a column name matches any base entity pattern.
     *
     * @param columnName the column name to check
     * @return true if the column should be extracted to BaseEntity
     */
    public boolean matches(String columnName) {
        for (String pattern : columnPatterns) {
            if (pattern.startsWith("^") && pattern.endsWith("$")) {
                if (columnName.matches(pattern)) {
                    return true;
                }
            } else {
                if (columnName.equalsIgnoreCase(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static BaseEntityConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = true;
        private String name = "BaseEntity";
        private List<String> columnPatterns = new ArrayList<>(List.of("id", "created_at", "updated_at"));

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder columnPatterns(String... patterns) {
            this.columnPatterns = new ArrayList<>(List.of(patterns));
            return this;
        }

        public Builder columnPatterns(List<String> patterns) {
            this.columnPatterns = new ArrayList<>(patterns);
            return this;
        }

        public BaseEntityConfig build() {
            return new BaseEntityConfig(this);
        }
    }
}
