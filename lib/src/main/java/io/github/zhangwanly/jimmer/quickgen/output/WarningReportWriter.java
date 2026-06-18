package io.github.zhangwanly.jimmer.quickgen.output;

import io.github.zhangwanly.jimmer.quickgen.analysis.SchemaWarning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Writes schema validation warnings to a Markdown report file.
 */
public final class WarningReportWriter {

    /**
     * Write schema validation warnings to a Markdown report file in the output directory.
     *
     * @param warnings  list of schema warnings to report
     * @param outputDir the root output directory
     */
    public void writeWarnings(List<SchemaWarning> warnings, Path outputDir) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("# Schema 验证报告\n\n");
        sb.append("> 生成时间: ").append(timestamp).append("\n");
        sb.append(">\n");
        sb.append("> 警告数量: ").append(warnings.size()).append("\n\n");

        sb.append("| # | 表 | 列 | 引用表 | 说明 |\n");
        sb.append("|---|---|---|---|---|\n");

        // Sort by extractedRef (referenced table name) alphabetically
        List<SchemaWarning> sorted = warnings.stream()
                .sorted(Comparator.comparing(SchemaWarning::extractedRef))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            SchemaWarning w = sorted.get(i);
            sb.append("| ").append(i + 1);
            sb.append(" | `").append(w.tableName()).append("`");
            sb.append(" | `").append(w.columnName()).append("`");
            sb.append(" | `").append(w.extractedRef()).append("`");
            sb.append(" | ").append(formatMessage(w));
            sb.append(" |\n");
        }

        sb.append("\n## 建议\n\n");
        sb.append("- 创建缺失的关联表\n");
        sb.append("- 若非外键则重命名该列\n");
        sb.append("- 若是行业术语，通过 `SchemaValidatorConfig` 加入白名单\n");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("schema-warnings.md"), sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write schema warnings report", e);
        }
    }

    private String formatMessage(SchemaWarning w) {
        return switch (w.type()) {
            case UNRESOLVED_FK -> "引用表不存在";
            case INCOMPLETE_MULTI_LEVEL -> String.format(
                    "疑似多层级外键，但仅有 1 列，约定要求至少 2 列（如 `%s1_id`, `%s2_id`）",
                    w.extractedRef(), w.extractedRef());
        };
    }
}
