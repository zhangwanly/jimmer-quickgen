package io.github.zhangwanly.jimmer.quickgen.output;

import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes JavaPoet JavaFile objects to the filesystem,
 * with post-processing to collapse multi-line annotations into compact single-line form.
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
                source = compactAnnotations(source);
                source = groupImports(source);

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

    /**
     * Collapse multi-line JavaPoet annotations into single-line form.
     * <p>For example, transforms:</p>
     * <pre>
     * &#64;JoinColumn(
     *     name = "store_id",
     *     foreignKeyType = ForeignKeyType.FAKE
     * )
     * </pre>
     * <p>into:</p>
     * <pre>
     * &#64;JoinColumn(name = "store_id", foreignKeyType = ForeignKeyType.FAKE)
     * </pre>
     */
    static String compactAnnotations(String source) {
        String[] lines = source.split("\n");
        List<String> result = new ArrayList<>();
        List<String> annotationLines = null;
        int parenDepth = 0;

        for (String line : lines) {
            if (annotationLines != null) {
                annotationLines.add(line);
                parenDepth += countParenDelta(line);
                if (parenDepth == 0) {
                    result.add(joinAnnotationLines(annotationLines));
                    annotationLines = null;
                }
            } else {
                String trimmed = line.trim();
                if (trimmed.startsWith("@") && countParenDelta(trimmed) > 0) {
                    annotationLines = new ArrayList<>();
                    annotationLines.add(line);
                    parenDepth = countParenDelta(trimmed);
                } else {
                    result.add(line);
                }
            }
        }

        if (annotationLines != null) {
            result.addAll(annotationLines);
        }

        return String.join("\n", result);
    }

    /**
     * Group imports in IDEA default style:
     * third-party packages first, then java.* / javax.*, separated by a blank line.
     */
    static String groupImports(String source) {
        String[] lines = source.split("\n");
        List<String> before = new ArrayList<>();
        List<String> thirdParty = new ArrayList<>();
        List<String> javaImports = new ArrayList<>();
        List<String> after = new ArrayList<>();

        int phase = 0; // 0=before imports, 1=in imports, 2=after imports
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                phase = 1;
                if (trimmed.startsWith("import java.") || trimmed.startsWith("import javax.")) {
                    javaImports.add(line);
                } else {
                    thirdParty.add(line);
                }
            } else if (phase == 1 && trimmed.isEmpty()) {
                // skip blank lines between imports
            } else {
                if (phase == 1) {
                    phase = 2;
                }
                if (phase == 2) {
                    after.add(line);
                } else {
                    before.add(line);
                }
            }
        }

        List<String> result = new ArrayList<>(before);
        if (!thirdParty.isEmpty()) {
            result.addAll(thirdParty);
        }
        if (!thirdParty.isEmpty() && !javaImports.isEmpty()) {
            result.add("");
        }
        if (!javaImports.isEmpty()) {
            result.addAll(javaImports);
        }
        // Add one blank line before the rest of the content
        int start = 0;
        while (start < after.size() && after.get(start).trim().isEmpty()) {
            start++;
        }
        if (start < after.size()) {
            result.add("");
            result.addAll(after.subList(start, after.size()));
        }

        return String.join("\n", result);
    }

    private static int countParenDelta(String line) {
        int delta = 0;
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '(') delta++;
                else if (c == ')') delta--;
            }
        }
        return delta;
    }

    private static String joinAnnotationLines(List<String> lines) {
        // Preserve the leading whitespace of the first line
        String firstLine = lines.get(0);
        String indent = firstLine.substring(0, firstLine.length() - firstLine.stripLeading().length());

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(trimmed);
        }
        // Remove spaces around parentheses: "( " -> "(", " )" -> ")"
        String result = sb.toString();
        result = result.replace("( ", "(").replace(" )", ")");
        return indent + result;
    }
}
