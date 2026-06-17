package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The result of BaseEntity extraction.
 *
 * @param baseColumns       columns that belong to BaseEntity
 * @param entityColumnNames map of table name → set of column names that remain in the entity
 */
public record BaseEntityExtractionResult(
        List<ColumnModel> baseColumns,
        Map<String, Set<String>> entityColumnNames
) {
    public boolean hasBaseEntity() {
        return !baseColumns.isEmpty();
    }

    public Set<String> getColumnNamesForTable(String tableName) {
        return entityColumnNames.getOrDefault(tableName, Set.of());
    }
}
