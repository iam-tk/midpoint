/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.indexing;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * TODO describe or refine/update/fix this concept ...
 */
public interface Normalization extends ValueNormalizer {

    @NotNull String getName();

    boolean isDefault();

    ItemName getIndexItemName();

    ItemPath getIndexItemPath();

    @NotNull PrismPropertyDefinition<String> getIndexItemDefinition();
}