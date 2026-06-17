package io.github.zhangwanly.jimmer.quickgen.analysis;

import io.github.zhangwanly.jimmer.quickgen.config.BaseEntityConfig;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;

import java.util.List;

/**
 * Strategy interface for extracting BaseEntity columns from tables.
 */
public interface BaseEntityExtractionStrategy {

    BaseEntityExtractionResult extract(List<TableModel> tables, BaseEntityConfig config);
}
