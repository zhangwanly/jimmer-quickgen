package io.github.zhangwanly.jimmer.quickgen.db;

import java.util.List;

/**
 * Strategy for introspecting database schema.
 * Implementations can use JDBC metadata, XML files, or any other source.
 */
public interface DatabaseIntrospectionStrategy {

    /**
     * Introspect the database and return a list of table models.
     *
     * @return list of all user tables with their columns and primary keys
     */
    List<TableModel> introspect();
}
