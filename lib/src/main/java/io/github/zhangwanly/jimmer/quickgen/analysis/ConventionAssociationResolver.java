package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.analysis.NamingConventions.DigitSuffixRef;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
        List<TableRefOverride> overrides = config.tableRefOverrides();

        Set<String> tableNames = new LinkedHashSet<>();
        for (TableModel t : nonJoinTables) {
            tableNames.add(t.tableName().toLowerCase());
        }

        Map<String, List<AssociationModel>> result = new LinkedHashMap<>();
        for (TableModel t : nonJoinTables) {
            result.put(t.tableName().toLowerCase(), new ArrayList<>());
        }

        // Phase 1: Pre-scan for digit suffix references ({table}{digit}_id pattern)
        // Collect columns that match digit suffix patterns and group by (sourceTable, targetTable)
        Map<String, Map<String, List<DigitSuffixColumn>>> digitSuffixGroups = new LinkedHashMap<>();
        Set<String> digitSuffixColumnKeys = new HashSet<>(); // "tableNameLower::columnName" keys to skip later

        for (TableModel table : nonJoinTables) {
            String tableNameLower = table.tableName().toLowerCase();
            Map<String, List<DigitSuffixColumn>> groups = new LinkedHashMap<>();

            for (ColumnModel col : table.columns()) {
                if (isBaseColumn(col.name(), baseColumnNames)) continue;

                // Skip exact match columns (they are handled by existing logic)
                Optional<String> exactRef = NamingConventions.extractReferenceTableName(col.name());
                if (exactRef.isPresent()) {
                    String effectiveRef = NamingConventions.resolveTableRefOverride(
                            tableNameLower, col.name().toLowerCase(), overrides).orElse(exactRef.get());
                    if (tableNames.contains(effectiveRef)) continue;
                }

                Optional<DigitSuffixRef> dsRef = NamingConventions.extractDigitSuffixRef(col.name(), tableNames);
                if (dsRef.isPresent()) {
                    String targetTable = dsRef.get().tableName();
                    groups.computeIfAbsent(targetTable, k -> new ArrayList<>())
                            .add(new DigitSuffixColumn(col, dsRef.get().digit()));
                }
            }

            digitSuffixGroups.put(tableNameLower, groups);
        }

        // Phase 2: Generate associations for valid digit suffix groups (>=2 columns per target)
        for (var sourceEntry : digitSuffixGroups.entrySet()) {
            String sourceTable = sourceEntry.getKey();

            for (var targetEntry : sourceEntry.getValue().entrySet()) {
                String targetTable = targetEntry.getKey();
                List<DigitSuffixColumn> cols = targetEntry.getValue();

                if (cols.size() < 2) continue; // Must have >=2 columns to trigger

                // Sort by digit to identify the leaf node (highest digit)
                cols.sort(Comparator.comparingInt(DigitSuffixColumn::digit));
                DigitSuffixColumn lastCol = cols.get(cols.size() - 1);

                String targetEntityName = NamingConventions.tableToEntityName(targetTable);

                // Generate ManyToOne for each digit suffix column
                for (DigitSuffixColumn dsCol : cols) {
                    ColumnModel col = dsCol.column();
                    String propName = NamingConventions.columnToPropertyName(stripIdSuffix(col.name()));

                    addAssociation(result, sourceTable,
                            new AssociationModel.ManyToOneAssoc(
                                    propName, targetEntityName, col.nullable(), col.name()));

                    // Mark this column as handled
                    digitSuffixColumnKeys.add(sourceTable + "::" + col.name());
                }

                // Generate ONE inverse OneToMany on target table, mappedBy to the last (leaf) column
                String sourceEntityName = NamingConventions.tableToEntityName(sourceTable);
                String lastPropName = NamingConventions.columnToPropertyName(stripIdSuffix(lastCol.column().name()));
                String inversePropName = NamingConventions.toListPropertyName(
                        NamingConventions.columnToPropertyName(sourceTable));

                addAssociation(result, targetTable,
                        new AssociationModel.OneToManyAssoc(
                                inversePropName, sourceEntityName, lastPropName));
            }
        }

        // Phase 3: Existing exact-match logic (skip digit suffix columns already handled)
        for (TableModel table : nonJoinTables) {
            String tableNameLower = table.tableName().toLowerCase();
            String entityName = NamingConventions.tableToEntityName(table.tableName());

            for (ColumnModel column : table.columns()) {
                if (isBaseColumn(column.name(), baseColumnNames)) continue;

                // Skip columns already handled as digit suffix FKs
                if (digitSuffixColumnKeys.contains(tableNameLower + "::" + column.name())) continue;

                Optional<String> refOpt = NamingConventions.extractReferenceTableName(column.name());
                if (refOpt.isEmpty()) continue;

                String refTable = refOpt.get();
                boolean isSelfRef = false;

                // Check for table reference override (legacy database corner case)
                Optional<String> overrideRef = NamingConventions.resolveTableRefOverride(
                        tableNameLower, column.name().toLowerCase(), overrides);
                if (overrideRef.isPresent()) {
                    refTable = overrideRef.get();
                }

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

    private record DigitSuffixColumn(ColumnModel column, int digit) {}

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
