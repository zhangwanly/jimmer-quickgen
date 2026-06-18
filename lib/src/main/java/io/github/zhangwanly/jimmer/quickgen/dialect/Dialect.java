package io.github.zhangwanly.jimmer.quickgen.dialect;

import io.github.zhangwanly.jimmer.quickgen.config.TypeMappingRegistry;

/**
 * Encapsulates database-specific type mapping knowledge.
 *
 * <p>Dialects register database-specific type mappings into a {@link TypeMappingRegistry}.
 * The registry uses a three-tier resolution: user override > dialect override > default mapping.</p>
 *
 * <p>Use {@link DialectFactory} to detect the appropriate dialect from a DataSource or product name.</p>
 */
public interface Dialect {

    /**
     * Returns the dialect name (e.g., "MySQL", "PostgreSQL", "Generic").
     */
    String name();

    /**
     * Applies dialect-specific type mappings to the registry.
     * <p>Dialect mappings have lower priority than user overrides.</p>
     *
     * @param registry the type mapping registry to enhance
     */
    void applyTypeMappings(TypeMappingRegistry registry);
}
