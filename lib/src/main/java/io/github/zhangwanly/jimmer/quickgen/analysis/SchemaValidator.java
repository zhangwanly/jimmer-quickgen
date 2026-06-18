package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.SchemaValidatorConfig;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.*;

/**
 * Validates schema conventions for FK-style columns.
 *
 * <p>Detects columns ending with {@code _id} that look like foreign key references
 * but don't match any known table. This helps identify naming inconsistencies.</p>
 *
 * <p>Columns in the whitelist (e.g., {@code open_id}, {@code session_id}) are excluded
 * from validation as they are known non-FK patterns.</p>
 */
public final class SchemaValidator implements SchemaValidationStrategy {

    private final SchemaValidatorConfig config;

    public SchemaValidator(SchemaValidatorConfig config) {
        this.config = config;
    }

    /**
     * Detect unresolved FK-style columns across all non-join tables.
     *
     * @param nonJoinTables   tables to validate
     * @param baseColumnNames columns to skip (BaseEntity columns)
     * @param associationMap  resolved associations (columns with associations are skipped)
     * @param tableNames      set of known table names (lowercase)
     * @return list of warnings for unresolved columns
     */
    @Override
    public List<SchemaWarning> validate(
            List<TableModel> nonJoinTables,
            Set<String> baseColumnNames,
            Map<String, List<AssociationModel>> associationMap,
            Set<String> tableNames) {

        // Build set of whitelisted column names (lowercase)
        Set<String> whitelist = new HashSet<>();
        for (String col : config.nonFkIdColumns()) {
            whitelist.add(col.toLowerCase());
        }

        // Build set of columns that already have associations (all lowercase for consistency)
        Set<String> resolvedColumns = new HashSet<>(); // "tableName::columnName" (lowercase)
        for (var entry : associationMap.entrySet()) {
            String tableName = entry.getKey().toLowerCase();
            for (AssociationModel assoc : entry.getValue()) {
                switch (assoc) {
                    case AssociationModel.ManyToOneAssoc m ->
                            resolvedColumns.add(tableName + "::" + m.joinColumnName().toLowerCase());
                    case AssociationModel.OneToOneOwningAssoc o ->
                            resolvedColumns.add(tableName + "::" + o.joinColumnName().toLowerCase());
                    default -> {}
                }
            }
        }

        List<SchemaWarning> warnings = new ArrayList<>();

        for (TableModel table : nonJoinTables) {
            String tableNameLower = table.tableName().toLowerCase();

            for (ColumnModel col : table.columns()) {
                String colNameLower = col.name().toLowerCase();

                // Skip base columns
                if (baseColumnNames.stream().anyMatch(bc -> bc.equalsIgnoreCase(colNameLower))) {
                    continue;
                }

                // Skip columns without _id suffix
                Optional<String> refOpt = NamingConventions.extractReferenceTableName(colNameLower);
                if (refOpt.isEmpty()) {
                    continue;
                }

                String extractedRef = refOpt.get();
                String key = tableNameLower + "::" + colNameLower;

                // Skip if already resolved as association
                if (resolvedColumns.contains(key)) {
                    continue;
                }

                // Skip if in whitelist
                if (whitelist.contains(colNameLower)) {
                    continue;
                }

                // Check if this is a digit suffix pattern that wasn't resolved
                // (e.g., lone category1_id without category2_id — resolver requires >= 2)
                Optional<NamingConventions.DigitSuffixRef> dsRef =
                        NamingConventions.extractDigitSuffixRef(colNameLower, tableNames);
                if (dsRef.isPresent()) {
                    warnings.add(new SchemaWarning(tableNameLower, colNameLower,
                            dsRef.get().tableName(), SchemaWarning.WarningType.INCOMPLETE_MULTI_LEVEL));
                    continue;
                }

                // This is an unresolved _id column
                warnings.add(new SchemaWarning(tableNameLower, colNameLower,
                        extractedRef, SchemaWarning.WarningType.UNRESOLVED_FK));
            }
        }

        return warnings;
    }
}
