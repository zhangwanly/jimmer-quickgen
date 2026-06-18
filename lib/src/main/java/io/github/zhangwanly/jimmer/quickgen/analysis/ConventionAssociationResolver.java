package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves associations between tables based on naming conventions.
 *
 * <p>Convention: a column named {@code {table_name}_id} references the table
 * named {@code {table_name}} and its primary key.</p>
 *
 * <p>Self-referencing: a column named like {@code parent_id} that does not match
 * any existing table name is treated as a self-reference if the pattern is configured
 * in {@link QuickGenConfig#selfRefPatterns()}.</p>
 *
 * <p>OneToOne: if the FK column is the sole primary key of the current table,
 * the association is {@code @OneToOne} instead of {@code @ManyToOne}.</p>
 *
 * <p>Implements {@link AssociationResolverStrategy} for DIP injection.</p>
 */
public final class ConventionAssociationResolver implements AssociationResolverStrategy {

    public ConventionAssociationResolver() {
    }

    @Override
    public Map<String, List<AssociationModel>> resolve(List<TableModel> nonJoinTables, Set<String> baseColumnNames, QuickGenConfig config) {
        List<String> selfRefPatterns = config.selfRefPatterns();

        Set<String> tableNames = new LinkedHashSet<>();
        for (TableModel t : nonJoinTables) {
            tableNames.add(t.tableName().toLowerCase());
        }

        Map<String, List<AssociationModel>> result = new LinkedHashMap<>();
        for (TableModel t : nonJoinTables) {
            result.put(t.tableName().toLowerCase(), new ArrayList<>());
        }

        for (TableModel table : nonJoinTables) {
            String tableNameLower = table.tableName().toLowerCase();
            String entityName = NamingConventions.tableToEntityName(table.tableName());

            for (ColumnModel column : table.columns()) {
                if (isBaseColumn(column.name(), baseColumnNames)) continue;

                Optional<String> refOpt = NamingConventions.extractReferenceTableName(column.name());
                if (refOpt.isEmpty()) continue;

                String refTable = refOpt.get();
                boolean isSelfRef = false;

                if (tableNames.contains(refTable)) {
                    if (refTable.equals(tableNameLower)) {
                        isSelfRef = true;
                    }
                } else if (isSelfRefPattern(column.name(), selfRefPatterns)) {
                    refTable = tableNameLower;
                    isSelfRef = true;
                } else {
                    continue;
                }

                String refEntityName = NamingConventions.tableToEntityName(refTable);
                String propName = NamingConventions.columnToPropertyName(
                        stripIdSuffix(column.name()));

                boolean isSolePk = isSolePrimaryKey(table, column.name());

                if (isSolePk && !isSelfRef) {
                    addAssociation(result, tableNameLower,
                            new AssociationModel.OneToOneOwningAssoc(
                                    propName, refEntityName, column.nullable(), column.name()));

                    String inversePropName = NamingConventions.columnToPropertyName(tableNameLower);
                    if (tableNames.contains(refTable) && !isSelfRef) {
                        addAssociation(result, refTable,
                                new AssociationModel.OneToOneInverseAssoc(
                                        inversePropName, entityName, column.nullable(), propName));
                    }
                } else {
                    addAssociation(result, tableNameLower,
                            new AssociationModel.ManyToOneAssoc(
                                    propName, refEntityName, column.nullable(), column.name()));

                    if (isSelfRef) {
                        String entityCamel = NamingConventions.columnToPropertyName(tableNameLower);
                        addAssociation(result, tableNameLower,
                                new AssociationModel.OneToManyAssoc(
                                        NamingConventions.selfRefInverseName(propName, entityCamel),
                                        entityName, propName));
                    } else {
                        String inversePropName = NamingConventions.toListPropertyName(
                                NamingConventions.columnToPropertyName(tableNameLower));
                        addAssociation(result, refTable,
                                new AssociationModel.OneToManyAssoc(
                                        inversePropName, entityName, propName));
                    }
                }
            }
        }

        return result;
    }

    private static boolean isBaseColumn(String columnName, Set<String> baseColumnNames) {
        return baseColumnNames.stream().anyMatch(bc -> bc.equalsIgnoreCase(columnName));
    }

    private static boolean isSelfRefPattern(String columnName, List<String> selfRefPatterns) {
        return selfRefPatterns.stream()
                .anyMatch(p -> p.equalsIgnoreCase(columnName));
    }

    private static boolean isSolePrimaryKey(TableModel table, String columnName) {
        return table.primaryKeyColumns().size() == 1
                && table.isPrimaryKeyColumn(columnName);
    }

    private static String stripIdSuffix(String columnName) {
        if (columnName.toLowerCase().endsWith("_id") && columnName.length() > 3) {
            return columnName.substring(0, columnName.length() - 3);
        }
        return columnName;
    }

    private static void addAssociation(Map<String, List<AssociationModel>> result, String tableName, AssociationModel model) {
        result.computeIfAbsent(tableName, k -> new ArrayList<>()).add(model);
    }
}
