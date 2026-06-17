package io.github.zhangwanly.jimmer.quickgen.db;

/**
 * Immutable representation of a database column.
 *
 * @param name           column name in lower_snake_case
 * @param typeName       JDBC type name (e.g. "VARCHAR", "BIGINT", "TIMESTAMP")
 * @param nullable       whether the column allows NULL values
 * @param autoIncrement  whether the column is auto-incremented
 * @param size           column size (for VARCHAR, DECIMAL, etc.)
 */
public record ColumnModel(
        String name,
        String typeName,
        boolean nullable,
        boolean autoIncrement,
        int size
) {
    public ColumnModel {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name must not be blank");
        }
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("Column typeName must not be blank");
        }
    }
}
