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

    public JoinTableDetector() {
        this(JoinTableConfig.defaults());
    }

    public JoinTableDetector(JoinTableConfig config) {
        this.config = config;
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

        for (ColumnModel col : table.columns()) {
            Optional<String> ref = NamingConventions.extractReferenceTableName(col.name());
            if (ref.isPresent() && allTableNames.contains(ref.get())) {
                fkRefCount++;
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
        for (ColumnModel col : joinTable.columns()) {
            Optional<String> ref = NamingConventions.extractReferenceTableName(col.name());
            if (ref.isPresent() && allTableNames.contains(ref.get())) {
                refs.add(ref.get());
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
