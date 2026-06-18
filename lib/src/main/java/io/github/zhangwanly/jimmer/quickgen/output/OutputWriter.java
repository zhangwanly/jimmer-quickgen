package io.github.zhangwanly.jimmer.quickgen.output;

import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes JavaPoet JavaFile objects to the filesystem,
 * applying post-processing (annotation compaction and import grouping) before writing.
 */
public final class OutputWriter {

    /**
     * Write all generated JavaFile objects to the specified output directory.
     *
     * @param files    list of JavaFile objects to write
     * @param outputDir the root output directory
     * @throws RuntimeException if any file fails to write
     */
    public void write(List<JavaFile> files, Path outputDir) {
        for (JavaFile file : files) {
            try {
                String source = file.toString();
                source = SourcePostProcessor.compactAnnotations(source);
                source = SourcePostProcessor.groupImports(source);

                Path dir = outputDir;
                if (!file.packageName.isEmpty()) {
                    dir = dir.resolve(file.packageName.replace('.', '/'));
                }
                Files.createDirectories(dir);
                Files.writeString(dir.resolve(file.typeSpec.name + ".java"), source);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write generated file", e);
            }
        }
    }
}
