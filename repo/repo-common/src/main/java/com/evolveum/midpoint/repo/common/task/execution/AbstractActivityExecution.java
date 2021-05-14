/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.execution;

import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.repo.common.task.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Base class for activity executions.
 *
 * @param <WD> Definition of the work that this activity has to do.
 */
public abstract class AbstractActivityExecution<WD extends WorkDefinition> implements ActivityExecution {

    /**
     * The task execution in context of which this activity execution takes place.
     */
    @NotNull protected final TaskExecution taskExecution;

    /**
     * Parent activity execution, or null if this is the root one.
     */
    protected final CompositeActivityExecution parent;

    /**
     * Definition of the activity. Contains the definition of the work.
     */
    @NotNull protected final ActivityDefinition<WD> activityDefinition;

    protected AbstractActivityExecution(ActivityContext<WD> context) {
        this.taskExecution = context.getTaskExecution();
        this.parent = context.getParentActivityExecution();
        this.activityDefinition = context.getActivityDefinition();
    }

    @NotNull
    @Override
    public TaskExecution getTaskExecution() {
        return taskExecution;
    }

    protected CommonTaskBeans getBeans() {
        return taskExecution.getBeans();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "activityDefinition=" + activityDefinition +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        if (parent == null) {
            DebugUtil.debugDumpWithLabelLn(sb, "task execution", taskExecution.shortDump(), indent + 1);
        }
        DebugUtil.debugDumpWithLabel(sb, "definition", activityDefinition, indent + 1);
        return sb.toString();
    }
}
