package io.github.zhangwanly.jimmer.quickgen.analysis;

/**
 * Sealed interface hierarchy for association models.
 * Each subtype carries only the fields relevant to its association type (ISP).
 */
public sealed interface AssociationModel {

    String propertyName();
    String targetEntityName();
    AssociationType type();

    record ManyToOneAssoc(
            String propertyName,
            String targetEntityName,
            boolean nullable,
            String joinColumnName
    ) implements AssociationModel {
        @Override public AssociationType type() { return AssociationType.MANY_TO_ONE; }
    }

    record OneToManyAssoc(
            String propertyName,
            String targetEntityName,
            String mappedBy
    ) implements AssociationModel {
        @Override public AssociationType type() { return AssociationType.ONE_TO_MANY; }
    }

    record OneToOneOwningAssoc(
            String propertyName,
            String targetEntityName,
            boolean nullable,
            String joinColumnName
    ) implements AssociationModel {
        @Override public AssociationType type() { return AssociationType.ONE_TO_ONE_OWNING; }
    }

    record OneToOneInverseAssoc(
            String propertyName,
            String targetEntityName,
            boolean nullable,
            String mappedBy
    ) implements AssociationModel {
        @Override public AssociationType type() { return AssociationType.ONE_TO_ONE_INVERSE; }
    }

    record ManyToManyOwningAssoc(
            String propertyName,
            String targetEntityName,
            String joinColumnName,
            String joinTableName,
            String inverseJoinColumnName
    ) implements AssociationModel {
        @Override public AssociationType type() { return AssociationType.MANY_TO_MANY_OWNING; }
    }

    record ManyToManyInverseAssoc(
            String propertyName,
            String targetEntityName,
            String mappedBy
    ) implements AssociationModel {
        @Override public AssociationType type() { return AssociationType.MANY_TO_MANY_INVERSE; }
    }
}
