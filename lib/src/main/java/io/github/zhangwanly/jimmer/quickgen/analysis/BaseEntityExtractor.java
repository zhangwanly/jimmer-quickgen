package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.BaseEntityConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.*;

/**
 * Extracts common columns from tables into a BaseEntity definition
 * based on configurable column name patterns.
 *
 * <p>Implements {@link BaseEntityExtractionStrategy} for DIP injection.</p>
 */
public final class BaseEntityExtractor implements BaseEntityExtractionStrategy {

    public BaseEntityExtractor() {}

    @Override
    public BaseEntityExtractionResult extract(List<TableModel> tables, BaseEntityConfig config) {
        if (!config.enabled()) {
            Map<String, Set<String>> allCols = new LinkedHashMap<>();
            for (TableModel table : tables) {
                Set<String> names = new LinkedHashSet<>();
                for (ColumnModel col : table.columns()) {
                    names.add(col.name());
                }
                allCols.put(table.tableName(), names);
            }
            return new BaseEntityExtractionResult(List.of(), allCols);
        }

        Map<String, ColumnModel> baseColumnMap = new LinkedHashMap<>();
        for (TableModel table : tables) {
            for (ColumnModel col : table.columns()) {
                if (config.matches(col.name())) {
                    baseColumnMap.putIfAbsent(col.name().toLowerCase(), col);
                }
            }
        }

        List<ColumnModel> baseColumns = new ArrayList<>(baseColumnMap.values());

        Map<String, Set<String>> entityColumnNames = new LinkedHashMap<>();
        for (TableModel table : tables) {
            Set<String> remaining = new LinkedHashSet<>();
            for (ColumnModel col : table.columns()) {
                if (!config.matches(col.name())) {
                    remaining.add(col.name());
                }
            }
            entityColumnNames.put(table.tableName(), remaining);
        }

        return new BaseEntityExtractionResult(baseColumns, entityColumnNames);
    }
}
