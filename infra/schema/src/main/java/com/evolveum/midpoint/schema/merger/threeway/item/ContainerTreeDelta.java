/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import java.util.Objects;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;

public class ContainerTreeDelta<C extends Containerable>
        extends ItemTreeDelta<PrismContainerValue<C>, PrismContainerDefinition<C>, PrismContainer<C>, ContainerTreeDeltaValue<C>> {

    public ContainerTreeDelta(PrismContainerDefinition<C> definition) {
        super(definition);
    }

    @Override
    protected String debugDumpShortName() {
        return "CTD";
    }

    @Override
    public ContainerTreeDeltaValue<C> createNewValue() {
        return new ContainerTreeDeltaValue<>();
    }

    public <D extends ItemTreeDelta> D findOrCreateItemDelta(ItemPath path, Class<D> deltaClass) {
        if (ItemPath.isEmpty(path)) {
            throw new IllegalArgumentException("Empty path specified");
        }

        Long id = path.firstToIdOrNull();
        ContainerTreeDeltaValue<C> val = findValue(id);
        if (val == null) {
            val = createNewValue();
            addValue(val);
        }

        ItemPath rest = path.startsWithId() ? path.rest() : path;
        return val.findOrCreateItemDelta(rest, deltaClass);
    }

    public ContainerTreeDeltaValue<C> findValue(Long id) {
        if (id == null) {
            if (getDefinition().isSingleValue()) {
                return getSingleValue();
            } else {
                throw new IllegalArgumentException("Attempt to get segment without an ID from a multi-valued container delta " + getItemName());
            }
        }

        return getValues().stream()
                .filter(v -> Objects.equals(id, v.getId()))
                .findFirst()
                .orElse(null);
    }
}
