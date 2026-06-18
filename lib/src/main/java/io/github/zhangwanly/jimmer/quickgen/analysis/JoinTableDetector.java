package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.JoinTableConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.*;

/**
 * Detects join tables (for ManyToMany associations) by analyzing column naming conventions.
 *
 * <p>A table is considered a join table when:</p>
 * <ul>
 *   <li>It has a composite primary key (2+ columns)</li>
 *   <li>ALL columns follow the {@code {table_name}_id} convention and reference existing tables
 *       (unless {@link JoinTableConfig#allowExtraNonFkColumns()} is true)</li>
 *   <li>It has at least {@link JoinTableConfig#requiredForeignKeyCount()} such reference columns</li>
 * </ul>
 *
 * <p>Implements {@link JoinTableDetectionStrategy} for DIP injection.</p>
 */
public final class JoinTableDetector implements JoinTableDetectionStrategy {

    private final JoinTableConfig config;
    private final List<TableRefOverride> overrides;

    public JoinTableDetector() {
        this(JoinTableConfig.defaults(), List.of());
    }

    public JoinTableDetector(JoinTableConfig config) {
        this(config, List.of());
    }

    public JoinTableDetector(JoinTableConfig config, List<TableRefOverride> overrides) {
        this.config = config;
        this.overrides = List.copyOf(overrides);
    }

    @Override
    public Set<String> detect(List<TableModel> tables, Set<String> allTableNames) {
        Set<String> joinTables = new LinkedHashSet<>();
        for (TableModel table : tables) {
            if (isJoinTable(table, allTableNames)) {
                joinTables.add(table.tableName().toLowerCase());
            }
        }
        return joinTables;
    }

    public boolean isJoinTable(TableModel table, Set<String> allTableNames) {
        if (!table.hasCompositePrimaryKey()) {
            return false;
        }

        int fkRefCount = 0;
        int totalColumns = table.columns().size();
        String tableNameLower = table.tableName().toLowerCase();

        for (ColumnModel col : table.columns()) {
            Optional<String> ref = NamingConventions.extractReferenceTableName(col.name());
            if (ref.isPresent()) {
                String effectiveRef = NamingConventions.resolveTableRefOverride(
                        tableNameLower, col.name().toLowerCase(), overrides).orElse(ref.get());
                if (allTableNames.contains(effectiveRef)) {
                    fkRefCount++;
                }
            }
        }

        if (fkRefCount < config.requiredForeignKeyCount()) {
            return false;
        }
        return config.allowExtraNonFkColumns() || fkRefCount == totalColumns;
    }

    @Override
    public List<String> getReferencedTables(TableModel joinTable, Set<String> allTableNames) {
        List<String> refs = new ArrayList<>();
        String tableNameLower = joinTable.tableName().toLowerCase();
        for (ColumnModel col : joinTable.columns()) {
            Optional<String> ref = NamingConventions.extractReferenceTableName(col.name());
            if (ref.isPresent()) {
                String effectiveRef = NamingConventions.resolveTableRefOverride(
                        tableNameLower, col.name().toLowerCase(), overrides).orElse(ref.get());
                if (allTableNames.contains(effectiveRef)) {
                    refs.add(effectiveRef);
                }
            }
        }
        Collections.sort(refs);
        return refs;
    }

    @Override
    public String getJoinColumnForRef(TableModel joinTable, String refTableName) {
        return refTableName + "_id";
    }
}
