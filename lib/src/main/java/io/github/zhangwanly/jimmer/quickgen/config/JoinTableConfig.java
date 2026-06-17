package io.github.zhangwanly.jimmer.quickgen.config;

/**
 * Configuration for join table detection criteria.
 *
 * @param requiredForeignKeyCount minimum number of FK reference columns (default 2)
 * @param allowExtraNonFkColumns whether non-FK columns are allowed in join tables (default false)
 */
public record JoinTableConfig(
        int requiredForeignKeyCount,
        boolean allowExtraNonFkColumns
) {
    public static JoinTableConfig defaults() {
        return new JoinTableConfig(2, false);
    }
}
