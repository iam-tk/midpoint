/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work.segmentation.content;

import static java.util.Collections.emptyList;

import java.util.List;
import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.task.work.ItemDefinitionProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NullWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

@Component
public class NullWorkBucketContentHandler extends BaseWorkBucketContentHandler {

    @PostConstruct
    public void register() {
        registry.registerHandler(null, this);
        registry.registerHandler(NullWorkBucketContentType.class, this);
    }

    @NotNull
    @Override
    public List<ObjectFilter> createSpecificFilters(@NotNull WorkBucketType bucket,
            AbstractWorkSegmentationType configuration, Class<? extends ObjectType> type,
            ItemDefinitionProvider itemDefinitionProvider) {
        return emptyList();
    }
}