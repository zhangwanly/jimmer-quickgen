package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy interface for schema validation.
 *
 * <p>Detects naming inconsistencies or unresolved FK-style columns
 * in the analyzed schema. Implementations can provide custom validation
 * rules or suppress validation entirely.</p>
 */
public interface SchemaValidationStrategy {

    /**
     * Validate the schema and return any warnings found.
     *
     * @param nonJoinTables   tables to validate (excluding join tables)
     * @param baseColumnNames columns to skip (BaseEntity columns)
     * @param associationMap  resolved associations (columns with associations are skipped)
     * @param tableNames      set of known table names (lowercase)
     * @return list of warnings for unresolved or suspicious columns
     */
    List<SchemaWarning> validate(
            List<TableModel> nonJoinTables,
            Set<String> baseColumnNames,
            Map<String, List<AssociationModel>> associationMap,
            Set<String> tableNames);
}
