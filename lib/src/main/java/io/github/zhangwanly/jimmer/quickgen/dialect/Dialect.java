package io.github.zhangwanly.jimmer.quickgen.dialect;

import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Encapsulates database-specific type mapping knowledge.
 *
 * <p>Dialects register database-specific type mappings into a {@link TypeMappingRegistry}.
 * The registry uses a three-tier resolution: user override > dialect override > default mapping.</p>
 */
public interface Dialect {

    /**
     * Returns the dialect name (e.g., "MySQL", "PostgreSQL", "Generic").
     */
    String name();

    /**
     * Applies dialect-specific type mappings to the registry.
     * <p>Dialect mappings have lower priority than user overrides.</p>
     *
     * @param registry the type mapping registry to enhance
     */
    void applyTypeMappings(TypeMappingRegistry registry);

    /**
     * Detects the database dialect from the given DataSource.
     *
     * @param dataSource the JDBC data source
     * @return the appropriate Dialect implementation, or a no-op GenericDialect for unknown databases
     * @throws RuntimeException if unable to connect or read database metadata
     */
    static Dialect detect(DataSource dataSource) {
        String productName;
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            productName = metaData.getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to detect database dialect", e);
        }
        return fromProductName(productName);
    }

    /**
     * Returns the appropriate Dialect based on the database product name.
     * <p>Matching is case-insensitive and uses contains() for flexibility.</p>
     */
    static Dialect fromProductName(String productName) {
        if (productName == null) {
            return new GenericDialect();
        }
        String lower = productName.toLowerCase();
        if (lower.contains("mysql")) {
            return new MysqlDialect();
        }
        if (lower.contains("postgresql") || lower.contains("postgres")) {
            return new PostgresDialect();
        }
        return new GenericDialect();
    }
}
