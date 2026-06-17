package io.github.zhangwanly.jimmer.quickgen.dialect;

import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;

/**
 * Generic dialect that applies no database-specific type mappings.
 * <p>Used for databases like H2, SQLite, or unknown databases where
 * the default type mappings are sufficient.</p>
 */
final class GenericDialect implements Dialect {

    @Override
    public String name() {
        return "Generic";
    }

    @Override
    public void applyTypeMappings(TypeMappingRegistry registry) {
        // No-op: relies on default mappings only
    }
}
