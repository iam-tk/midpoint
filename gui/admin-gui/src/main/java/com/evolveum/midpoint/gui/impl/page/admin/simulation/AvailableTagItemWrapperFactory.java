/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.factory.AbstractSearchItemWrapperFactory;
import com.evolveum.midpoint.gui.impl.component.search.factory.SearchItemContext;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AvailableTagItemWrapperFactory extends AbstractSearchItemWrapperFactory<String, AvailableTagSearchItemWrapper> {

    @Override
    protected AvailableTagSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        SearchContext additionalSearchContext = ctx.getAdditionalSearchContext();
        List<DisplayableValue<String>> availableEventTags = additionalSearchContext != null ?
                additionalSearchContext.getAvailableEventTags() : new ArrayList<>();

        return new AvailableTagSearchItemWrapper(availableEventTags);
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return SimulationResultProcessedObjectType.F_EVENT_TAG_REF.equivalent(ctx.getPath());
    }
}
