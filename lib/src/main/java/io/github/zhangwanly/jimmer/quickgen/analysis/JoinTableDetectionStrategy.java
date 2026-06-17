package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.List;
import java.util.Set;

/**
 * Strategy interface for detecting join tables (for ManyToMany associations).
 */
public interface JoinTableDetectionStrategy {

    Set<String> detect(List<TableModel> tables, Set<String> allTableNames);

    List<String> getReferencedTables(TableModel joinTable, Set<String> allTableNames);

    String getJoinColumnForRef(TableModel joinTable, String refTableName);
}
