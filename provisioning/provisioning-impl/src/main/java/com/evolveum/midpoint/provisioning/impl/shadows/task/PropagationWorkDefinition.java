/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PropagationWorkDefinition extends AbstractWorkDefinition {

    @NotNull private final String resourceOid;

    PropagationWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (PropagationWorkDefinitionType) info.getBean();
        ObjectReferenceType resourceRef = typedDefinition.getResourceRef();
        resourceOid = MiscUtil.configNonNull(
                resourceRef != null ? resourceRef.getOid() : null,
                "No resource OID specified");
    }

    public @NotNull String getResourceOid() {
        return resourceOid;
    }

    @Override
    public @Nullable TaskAffectedObjectsType getAffectedObjects() {
        return new TaskAffectedObjectsType()
                .activity(new ActivityAffectedObjectsType()
                        .activityType(getActivityTypeName())
                        .resourceObjects(new BasicResourceObjectSetType()
                                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE))
                );
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "resourceOid", resourceOid, indent+1);
    }
}
