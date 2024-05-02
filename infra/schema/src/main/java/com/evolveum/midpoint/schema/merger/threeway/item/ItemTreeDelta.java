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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

// todo visitable
public abstract class ItemTreeDelta
        <
                PV extends PrismValue,
                ID extends ItemDefinition<I>,
                I extends Item<PV, ID>,
                V extends ItemTreeDeltaValue
                >
        implements DebugDumpable {

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
}
