package io.github.zhangwanly.jimmer.quickgen.analysis;

/**
 * Types of entity associations supported by the code generator.
 */
public enum AssociationType {
    MANY_TO_ONE,
    ONE_TO_MANY,
    ONE_TO_ONE_OWNING,
    ONE_TO_ONE_INVERSE,
    MANY_TO_MANY_OWNING,
    MANY_TO_MANY_INVERSE
}
