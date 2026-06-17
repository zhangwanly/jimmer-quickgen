package io.github.zhangwanly.jimmer.quickgen.db;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Default JDBC-based database introspector.
 * Reads tables, columns, and primary keys from DatabaseMetaData.
 */
public final class JdbcMetadataIntrospector implements DatabaseIntrospectionStrategy {

    private final DataSource dataSource;

    public JdbcMetadataIntrospector(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public List<TableModel> introspect() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = conn.getSchema();

            List<String> tableNames = readTableNames(meta, catalog, schema);
            List<TableModel> tables = new ArrayList<>();

            for (String tableName : tableNames) {
                List<ColumnModel> columns = readColumns(meta, catalog, schema, tableName);
                Set<String> pkColumns = readPrimaryKeys(meta, catalog, schema, tableName);
                tables.add(new TableModel(tableName, columns, pkColumns));
            }

            return tables;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to introspect database metadata", e);
        }
    }

    private List<String> readTableNames(DatabaseMetaData meta, String catalog, String schema) throws SQLException {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = meta.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                names.add(rs.getString("TABLE_NAME"));
            }
        }
        return names;
    }

    private List<ColumnModel> readColumns(DatabaseMetaData meta, String catalog, String schema, String tableName)
            throws SQLException {
        List<ColumnModel> columns = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME").toUpperCase();
                boolean nullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                boolean autoIncrement = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                int size = rs.getInt("COLUMN_SIZE");
                columns.add(new ColumnModel(name, typeName, nullable, autoIncrement, size));
            }
        }
        return columns;
    }

    private Set<String> readPrimaryKeys(DatabaseMetaData meta, String catalog, String schema, String tableName)
            throws SQLException {
        Set<String> pkColumns = new LinkedHashSet<>();
        try (ResultSet rs = meta.getPrimaryKeys(catalog, schema, tableName)) {
            while (rs.next()) {
                pkColumns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return pkColumns;
    }
}
