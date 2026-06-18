package io.github.zhangwanly.jimmer.quickgen.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Configuration for schema validation.
 *
 * <p>Defines a whitelist of column names that end with {@code _id} but are
 * NOT foreign key references (e.g., {@code open_id}, {@code session_id}).</p>
 *
 * <p>Default whitelist includes common industry terms.</p>
 */
public final class SchemaValidatorConfig {

    /**
     * Default non-FK column names that end with _id.
     * These are industry-standard terms, not foreign key references.
     */
    private static final List<String> DEFAULT_NON_FK_ID_COLUMNS = List.of(
            "open_id",       // WeChat OpenID
            "union_id",      // WeChat UnionID
            "session_id",    // Session identifier
            "request_id",    // Request tracking ID
            "trace_id",      // Distributed tracing ID
            "app_id",        // Application ID (WeChat, etc.)
            "correlation_id" // Correlation ID for distributed systems
    );

    private final List<String> nonFkIdColumns;

    private SchemaValidatorConfig(List<String> nonFkIdColumns) {
        this.nonFkIdColumns = List.copyOf(nonFkIdColumns);
    }

    public List<String> nonFkIdColumns() {
        return nonFkIdColumns;
    }

    public static SchemaValidatorConfig defaults() {
        return new SchemaValidatorConfig(DEFAULT_NON_FK_ID_COLUMNS);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<String> extraColumns = new ArrayList<>();
        private boolean replaceDefaults = false;
        private final List<String> customColumns = new ArrayList<>();

        private Builder() {}

        /**
         * Add additional non-FK column names to the whitelist.
         */
        public Builder addNonFkIdColumns(String... columns) {
            for (String col : columns) {
                extraColumns.add(col.toLowerCase());
            }
            return this;
        }

        /**
         * Replace default whitelist entirely with custom list.
         */
        public Builder customNonFkIdColumns(List<String> columns) {
            replaceDefaults = true;
            customColumns.clear();
            customColumns.addAll(columns.stream().map(String::toLowerCase).toList());
            return this;
        }

        public SchemaValidatorConfig build() {
            List<String> finalList = new ArrayList<>();
            if (!replaceDefaults) {
                finalList.addAll(DEFAULT_NON_FK_ID_COLUMNS);
            }
            finalList.addAll(replaceDefaults ? customColumns : extraColumns);
            return new SchemaValidatorConfig(finalList);
        }
    }
}
