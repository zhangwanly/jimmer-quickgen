package io.github.zhangwanly.jimmer.quickgen;

import io.github.zhangwanly.jimmer.quickgen.codegen.JimmerCodeGenerator;
import io.github.zhangwanly.jimmer.quickgen.config.QuickGenConfig;
import io.github.zhangwanly.jimmer.quickgen.db.DatabaseIntrospector;
import io.github.zhangwanly.jimmer.quickgen.db.TableModel;
import io.github.zhangwanly.jimmer.quickgen.dialect.Dialect;
import io.github.zhangwanly.jimmer.quickgen.dialect.DialectFactory;
import io.github.zhangwanly.jimmer.quickgen.output.OutputWriter;
import io.github.zhangwanly.jimmer.quickgen.output.WarningReportWriter;
import com.squareup.javapoet.JavaFile;

import javax.sql.DataSource;
import java.util.List;
/**
 * Main facade / entry point for the Jimmer QuickGen reverse engineering process.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * QuickGenConfig config = QuickGenConfig.builder()
 *     .basePackage("com.example.entity")
 *     .outputDir(Path.of("generated"))
 *     .build();
 *
 * QuickGen.generate(dataSource, config);
 * }</pre>
 */
public final class QuickGen {

    private QuickGen() {}

    /**
     * Run the full reverse engineering pipeline:
     * detect dialect → introspect database → analyze schema → generate entity code → write files.
     *
     * @param dataSource JDBC DataSource to introspect
     * @param config     generation configuration
     * @return list of generated JavaFile objects
     */
    public static List<JavaFile> generate(DataSource dataSource, QuickGenConfig config) {
        // Step 0: Detect database dialect and apply type mappings
        Dialect dialect = DialectFactory.detect(dataSource);
        dialect.applyTypeMappings(config.typeMappingRegistry());

        // Step 1: Introspect database (tables, columns, PKs — no FKs)
        DatabaseIntrospector introspector = new DatabaseIntrospector(dataSource);
        List<TableModel> tables = introspector.introspect();

        // Step 2: Generate code (full pipeline: BaseEntity + associations + ManyToMany)
        JimmerCodeGenerator generator = new JimmerCodeGenerator(config);
        JimmerCodeGenerator.GenerationResult result = generator.generateFull(tables);

        // Step 3: Write to filesystem
        OutputWriter writer = new OutputWriter();
        writer.write(result.files(), config.outputDir());

        // Step 4: Write schema validation warnings (if any)
        if (result.schema().hasWarnings()) {
            new WarningReportWriter().writeWarnings(result.schema().warnings(), config.outputDir());
        }

        return result.files();
    }
}
