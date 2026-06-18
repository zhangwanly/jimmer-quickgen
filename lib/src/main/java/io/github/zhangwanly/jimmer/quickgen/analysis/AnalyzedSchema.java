package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.*;

/**
 * The complete analyzed schema, ready for code generation.
 * Holds all intermediate results from the analysis pipeline so that
 * the code generator only needs to produce JavaFile objects.
 *
 * @param baseColumns       BaseEntity columns (empty if BaseEntity disabled)
 * @param basePkColumns     primary key column names within BaseEntity
 * @param baseColumnNames   all base column names for filtering
 * @param entityModels      resolved entity models (excluding join tables)
 * @param joinTableNames    names of tables identified as join tables
 * @param tableMap          lowercase-keyed table lookup
 * @param warnings          schema validation warnings (unresolved _id columns)
 */
public record AnalyzedSchema(
        List<ColumnModel> baseColumns,
        Set<String> basePkColumns,
        Set<String> baseColumnNames,
        List<EntityModel> entityModels,
        Set<String> joinTableNames,
        Map<String, List<AssociationModel>> associationMap,
        Map<String, TableModel> tableMap,
        List<SchemaWarning> warnings
) {
    public AnalyzedSchema {
        baseColumns = List.copyOf(baseColumns);
        basePkColumns = Set.copyOf(basePkColumns);
        baseColumnNames = Set.copyOf(baseColumnNames);
        entityModels = List.copyOf(entityModels);
        joinTableNames = Set.copyOf(joinTableNames);
        associationMap = Map.copyOf(associationMap);
        tableMap = Map.copyOf(tableMap);
        warnings = List.copyOf(warnings);
    }

    public boolean hasBaseEntity() {
        return !baseColumns.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
