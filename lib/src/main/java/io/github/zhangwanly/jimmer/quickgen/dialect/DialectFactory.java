package io.github.zhangwanly.jimmer.quickgen.dialect;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Factory for creating {@link Dialect} instances based on database product name.
 *
 * <p>Detects the appropriate dialect from a {@link DataSource} or a product name string,
 * returning {@link MysqlDialect}, {@link PostgresDialect}, or {@link GenericDialect}
 * as appropriate.</p>
 */
public final class DialectFactory {

    private DialectFactory() {}

    /**
     * Detects the database dialect from the given DataSource.
     *
     * @param dataSource the JDBC data source
     * @return the appropriate Dialect implementation, or a no-op GenericDialect for unknown databases
     * @throws RuntimeException if unable to connect or read database metadata
     */
    public static Dialect detect(DataSource dataSource) {
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
    public static Dialect fromProductName(String productName) {
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
