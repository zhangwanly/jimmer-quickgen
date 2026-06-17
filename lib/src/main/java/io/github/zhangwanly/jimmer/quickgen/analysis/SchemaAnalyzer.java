package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;
import io.github.zhangwanly.jimmer.quickgen.db.ColumnModel;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.*;

/**
 * Analyzes database tables to produce a complete {@link AnalyzedSchema}.
 * This is the "analysis" half of the pipeline, separated from code generation (SRP).
 *
 * <p>Accepts pluggable strategies via constructor (DIP). Defaults are provided
 * so callers that don't need custom behavior can use the single-arg constructor.</p>
 *
 * <p>Pipeline:</p>
 * <ol>
 *   <li>Extract BaseEntity columns</li>
 *   <li>Detect join tables</li>
 *   <li>Filter out join tables</li>
 *   <li>Resolve associations by naming convention</li>
 *   <li>Resolve ManyToMany from join tables</li>
 *   <li>Build EntityModel for each non-join table</li>
 * </ol>
 */
public final class SchemaAnalyzer {

    private final QuickGenConfig config;
    private final BaseEntityExtractionStrategy baseEntityExtractor;
    private final JoinTableDetectionStrategy joinTableDetector;
    private final AssociationResolverStrategy associationResolver;

    public SchemaAnalyzer(QuickGenConfig config) {
        this(config,
             new BaseEntityExtractor(),
             new JoinTableDetector(config.joinTableConfig()),
             new ConventionAssociationResolver()
        );
    }

    public SchemaAnalyzer(QuickGenConfig config,
                          BaseEntityExtractionStrategy baseEntityExtractor,
                          JoinTableDetectionStrategy joinTableDetector,
                          AssociationResolverStrategy associationResolver) {
        this.config = config;
        this.baseEntityExtractor = baseEntityExtractor;
        this.joinTableDetector = joinTableDetector;
        this.associationResolver = associationResolver;
    }

    /**
     * Run the full analysis pipeline on the given tables.
     */
    public AnalyzedSchema analyze(List<TableModel> tables) {
        // All internal table name keys are lowercase for case-insensitive consistency
        Set<String> allTableNames = new LinkedHashSet<>();
        Map<String, TableModel> tableMap = new LinkedHashMap<>();
        for (TableModel t : tables) {
            String key = t.tableName().toLowerCase();
            allTableNames.add(key);
            tableMap.put(key, t);
        }

        // Step 1: Extract BaseEntity columns
        BaseEntityExtractionResult baseResult = baseEntityExtractor.extract(tables, config.baseEntityConfig());
        Set<String> baseColumnNames = new LinkedHashSet<>();
        Set<String> basePkColumns = new LinkedHashSet<>();
        for (ColumnModel col : baseResult.baseColumns()) {
            baseColumnNames.add(col.name());
        }
        for (TableModel t : tables) {
            for (String pkCol : t.primaryKeyColumns()) {
                if (baseColumnNames.stream().anyMatch(bc -> bc.equalsIgnoreCase(pkCol))) {
                    basePkColumns.add(pkCol);
                }
            }
        }

        // Step 2: Detect join tables (returns lowercase names)
        Set<String> joinTableNames = joinTableDetector.detect(tables, allTableNames);

        // Step 3: Filter out join tables
        List<TableModel> nonJoinTables = tables.stream()
                .filter(t -> !joinTableNames.contains(t.tableName().toLowerCase()))
                .toList();

        // Step 4: Resolve associations (returns lowercase keys)
        Map<String, List<AssociationModel>> associationMap =
                associationResolver.resolve(nonJoinTables, baseColumnNames, config);

        // Step 5: Resolve ManyToMany from join tables
        resolveManyToMany(tableMap, joinTableNames, allTableNames, associationMap);

        // Step 6: Build EntityModel for each non-join table
        List<EntityModel> entityModels = buildEntityModels(
                nonJoinTables, baseResult, baseColumnNames, associationMap);

        return new AnalyzedSchema(
                baseResult.baseColumns(),
                basePkColumns,
                baseColumnNames,
                entityModels,
                joinTableNames,
                associationMap,
                tableMap);
    }

    private List<EntityModel> buildEntityModels(
            List<TableModel> nonJoinTables,
            BaseEntityExtractionResult baseResult,
            Set<String> baseColumnNames,
            Map<String, List<AssociationModel>> associationMap) {

        TypeMappingRegistry registry = config.typeMappingRegistry();
        List<EntityModel> entityModels = new ArrayList<>();

        for (TableModel table : nonJoinTables) {
            String entityName = NamingConventions.tableToEntityName(table.tableName());

            List<PropertyModel> scalars = new ArrayList<>();
            for (ColumnModel col : table.columns()) {
                if (baseColumnNames.stream().anyMatch(bc -> bc.equalsIgnoreCase(col.name()))) {
                    continue;
                }
                boolean isPk = table.isPrimaryKeyColumn(col.name());
                scalars.add(new PropertyModel(
                        NamingConventions.columnToPropertyName(col.name()),
                        col.name(),
                        registry.resolve(col.name(), col.typeName(), col.nullable()),
                        col.nullable(),
                        isPk));
            }

            String key = table.tableName().toLowerCase();
            List<AssociationModel> associations = associationMap.getOrDefault(key, List.of());

            boolean extendsBaseEntity = baseResult.hasBaseEntity()
                    && table.columns().stream()
                            .anyMatch(col -> baseColumnNames.stream()
                                    .anyMatch(bc -> bc.equalsIgnoreCase(col.name())));

            entityModels.add(new EntityModel(table.tableName(), entityName, scalars, associations, extendsBaseEntity));
        }

        return entityModels;
    }

    private void resolveManyToMany(Map<String, TableModel> tableMap,
                                    Set<String> joinTableNames,
                                    Set<String> allTableNames,
                                    Map<String, List<AssociationModel>> associationMap) {
        for (String joinTableName : joinTableNames) {
            TableModel joinTable = tableMap.get(joinTableName);
            if (joinTable == null) continue;

            List<String> refs = joinTableDetector.getReferencedTables(joinTable, allTableNames);
            if (refs.size() != 2) continue;

            String owningTable = refs.get(0);
            String inverseTable = refs.get(1);

            String owningEntityName = NamingConventions.tableToEntityName(owningTable);
            String inverseEntityName = NamingConventions.tableToEntityName(inverseTable);

            String owningJoinColumn = joinTableDetector.getJoinColumnForRef(joinTable, owningTable);
            String inverseJoinColumn = joinTableDetector.getJoinColumnForRef(joinTable, inverseTable);

            String owningPropName = NamingConventions.pluralize(
                    NamingConventions.columnToPropertyName(inverseTable));

            if (tableMap.containsKey(owningTable)) {
                associationMap.computeIfAbsent(owningTable, k -> new ArrayList<>())
                        .add(new AssociationModel.ManyToManyOwningAssoc(
                                owningPropName,
                                inverseEntityName,
                                owningJoinColumn,
                                joinTableName,
                                inverseJoinColumn));
            }

            String inversePropName = NamingConventions.pluralize(
                    NamingConventions.columnToPropertyName(owningTable));

            if (tableMap.containsKey(inverseTable)) {
                associationMap.computeIfAbsent(inverseTable, k -> new ArrayList<>())
                        .add(new AssociationModel.ManyToManyInverseAssoc(
                                inversePropName,
                                owningEntityName,
                                owningPropName));
            }
        }
    }
}
