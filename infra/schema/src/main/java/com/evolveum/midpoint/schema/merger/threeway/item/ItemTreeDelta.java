/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathComparatorUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public abstract class ItemTreeDelta
        <
                PV extends PrismValue,
                ID extends ItemDefinition<I>,
                I extends Item<PV, ID>,
                V extends ItemTreeDeltaValue
                >
        implements DebugDumpable, Visitable {

    private ContainerTreeDeltaValue<?> parent;

    private ID definition;

    private List<V> values;

    public ItemTreeDelta(ID definition) {
        this.definition = definition;
    }

    @NotNull
    public ID getDefinition() {
        return definition;
    }

    public void setDefinition(ID definition) {
        this.definition = definition;
    }

    public QName getItemName() {
        return definition.getItemName();
    }

    public QName getTypeName() {
        return definition.getTypeName();
    }

    public ContainerTreeDeltaValue<?> getParent() {
        return parent;
    }

    public void setParent(ContainerTreeDeltaValue<?> parent) {
        this.parent = parent;
    }

    @NotNull
    public List<V> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }

    public V getSingleValue() {
        List<V> values = getValues();
        if (values.size() > 1) {
            throw new IllegalStateException("More than one value in delta for " + getItemName());
        } else if (values.isEmpty()) {
            return null;
        } else {
            return values.get(0);
        }
    }

    public void setValues(@NotNull List<V> values) {
        this.values = values;
    }

    public void addValue(@NotNull V value) {
        value.setParent(this);

        getValues().add(value);
    }

    public void removeValue(@NotNull V value) {
        int index = getValues().indexOf(value);
        if (index < 0) {
            return;
        }

        V removed = getValues().remove(index);
        removed.setParent(null);
    }

    public abstract V createNewValue();

    @Override
    public String toString() {
        return debugDump();
    }

    protected String debugDumpShortName() {
        return getClass().getSimpleName();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.debugDumpWithLabelLn(sb, debugDumpShortName(), DebugUtil.formatElementName(getItemName()), indent);
        DebugUtil.debugDumpWithLabel(sb, "values", getValues(), indent + 1);

        return sb.toString();
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);

        getValues().forEach(value -> value.accept(visitor));
    }

    public ItemPath getPath() {
        if (parent == null) {
            return ItemPath.create(getItemName());
        }

        return parent.getPath().append(getItemName());
    }

    @NotNull
    public List<Conflict> getConflictsWith(ItemTreeDelta<PV, ID, I, V> other, EquivalenceStrategy strategy) {
        if (other == null) {
            return List.of();
        }

        if (!ItemPathComparatorUtil.equivalent(getPath(), other.getPath())) {
            return List.of();
        }

        if (definition.isSingleValue()) {
            V value = getSingleValue();

            V otherValue = other.getSingleValue();
            if (value == null && otherValue == null) {
                return List.of();
            }

            if (value == null || otherValue == null) {
                return List.of(new Conflict(value, otherValue));
            }

            return value.getConflictsWith(otherValue, strategy);
        }

        List<Conflict> conflicts = new ArrayList<>();
        for (V value : getValues()) {
            V otherValue = other.findMatchingValue(value, strategy);
            if (otherValue == null) {
                continue;
            }

            List<Conflict> valueConflicts = value.getConflictsWith(otherValue, strategy);
            conflicts.addAll(valueConflicts);
        }

        return conflicts;
    }

    @NotNull
    public List<Conflict> getConflictsWith(ItemTreeDelta<PV, ID, I, V> other) {
        return getConflictsWith(other, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
    }

    public boolean hasConflictWith(ItemTreeDelta<PV, ID, I, V> other, EquivalenceStrategy strategy) {
        return !getConflictsWith(other, strategy).isEmpty();
    }

    public boolean hasConflictWith(ItemTreeDelta<PV, ID, I, V> other) {
        return hasConflictWith(other, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
    }

    // todo use strategy
    protected V findMatchingValue(V other, EquivalenceStrategy strategy) {
        if (definition.isSingleValue()) {
            return getSingleValue();
        }

        for (V value : getValues()) {
            if (value.match(other)) {
                return value;
            }
        }

        return null;
    }

    public boolean containsModifications() {
        return getValues().stream().anyMatch(V::containsModifications);
    }
}
