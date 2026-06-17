package io.github.zhangwanly.jimmer.quickgen.db;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable representation of a database table.
 *
 * @param tableName       table name in lower_snake_case
 * @param columns         ordered list of columns
 * @param primaryKeyColumns set of column names that form the primary key
 */
public record TableModel(
        String tableName,
        List<ColumnModel> columns,
        Set<String> primaryKeyColumns
) {
    public TableModel {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Table name must not be blank");
        }
        columns = List.copyOf(columns);
        primaryKeyColumns = Set.copyOf(primaryKeyColumns);
    }

    public boolean hasCompositePrimaryKey() {
        return primaryKeyColumns.size() > 1;
    }

    public boolean isPrimaryKeyColumn(String columnName) {
        return primaryKeyColumns.stream()
                .anyMatch(pk -> pk.equalsIgnoreCase(columnName));
    }

    public Optional<ColumnModel> getColumn(String name) {
        return columns.stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst();
    }
}
