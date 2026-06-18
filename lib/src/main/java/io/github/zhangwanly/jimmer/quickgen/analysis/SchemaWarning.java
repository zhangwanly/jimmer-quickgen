package io.github.zhangwanly.jimmer.quickgen.analysis;

/**
 * 表示未解析的 FK 风格列的 schema 验证警告。
 *
 * @param tableName    包含该列的表名（小写）
 * @param columnName   未解析的列名（小写）
 * @param extractedRef 从列名中提取的表名（去除 _id 后缀）
 * @param type         警告类型
 */
public record SchemaWarning(
        String tableName,
        String columnName,
        String extractedRef,
        WarningType type
) {
    /**
     * 警告类型枚举
     */
    public enum WarningType {
        /** 普通 _id 列，但找不到对应表 */
        UNRESOLVED_FK,
        /** digit suffix 模式（如 category1_id），但不足 2 列，不符合多层级 FK 约定 */
        INCOMPLETE_MULTI_LEVEL
    }
}
