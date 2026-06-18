package io.github.zhangwanly.jimmer.quickgen.analysis;

/**
 * A manual override for FK column reference resolution in legacy databases
 * where the column name does not follow the strict {@code {table_name}_id} convention.
 *
 * <p>Example: {@code order_info.user_id} actually references {@code user_info} table,
 * not {@code user} table. An override {@code ("order_info", "user_id", "user_info")}
 * tells the resolver to look at {@code user_info} instead of the naively extracted {@code user}.</p>
 *
 * @param tableName      the table that owns the FK column (lowercase), e.g. "order_info"
 * @param columnName     the FK column name (lowercase), e.g. "user_id"
 * @param actualRefTable the actual referenced table name (lowercase), e.g. "user_info"
 */
public record TableRefOverride(
        String tableName,
        String columnName,
        String actualRefTable
) {
    public TableRefOverride {
        tableName = tableName.toLowerCase();
        columnName = columnName.toLowerCase();
        actualRefTable = actualRefTable.toLowerCase();
    }
}
