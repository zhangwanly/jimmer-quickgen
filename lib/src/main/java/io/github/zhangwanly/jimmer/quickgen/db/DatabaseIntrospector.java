package io.github.zhangwanly.jimmer.quickgen.db;

import javax.sql.DataSource;
import java.util.*;

/**
 * Facade for database introspection.
 * Delegates to a {@link DatabaseIntrospectionStrategy} for the actual implementation.
 *
 * <p>For backward compatibility, accepts a {@link DataSource} and uses
 * {@link JdbcMetadataIntrospector} as the default strategy.</p>
 */
public class DatabaseIntrospector {

    private final DatabaseIntrospectionStrategy strategy;

    public DatabaseIntrospector(DataSource dataSource) {
        this(new JdbcMetadataIntrospector(dataSource));
    }

    public DatabaseIntrospector(DatabaseIntrospectionStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
    }

    /**
     * Introspect the database and return a list of table models.
     *
     * @return list of all user tables with their columns and primary keys
     */
    public List<TableModel> introspect() {
        return strategy.introspect();
    }
}
