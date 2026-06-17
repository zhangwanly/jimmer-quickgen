package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy interface for resolving associations between tables.
 */
public interface AssociationResolverStrategy {
    Map<String, List<AssociationModel>> resolve(List<TableModel> nonJoinTables, Set<String> baseColumnNames, QuickGenConfig config);
}
