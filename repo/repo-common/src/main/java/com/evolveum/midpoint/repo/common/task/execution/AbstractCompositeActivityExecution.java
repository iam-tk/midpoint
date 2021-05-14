/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.execution;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract superclass for both pure- and semi-composite activities.
 *
 * @param <WD> Type of work definition.
 */
public abstract class AbstractCompositeActivityExecution<WD extends WorkDefinition>
        extends AbstractActivityExecution<WD>
        implements CompositeActivityExecution {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCompositeActivityExecution.class);

    @NotNull protected final List<ActivityExecution> children = new ArrayList<>();

    /**
     * Execution result from the last activity executed so far.
     */
    private ActivityExecutionResult executionResult;

    protected AbstractCompositeActivityExecution(ActivityContext<WD> context) {
        super(context);
    }

    @Override
    public @NotNull ActivityExecutionResult execute(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException {

        LOGGER.trace("Before children creation:\n{}", debugDumpLazily());

        List<ActivityExecution> children = createChildren(result);
        this.children.addAll(children);
        tailorChildren(result);

        LOGGER.trace("After children creation, before execution:\n{}", debugDumpLazily());

        executeChildren(result);

        LOGGER.trace("After children execution ({}):\n{}", executionResult.shortDumpLazily(), debugDumpLazily());

        return executionResult;
    }

    /**
     * Executes tailoring instructions, i.e. inserts new activities before/after specified ones,
     * or changes the configuration of specified activities.
     */
    private void tailorChildren(OperationResult result) {
        // TODO
    }

    /**
     * Creates child activity executions: either explicitly specified by the configuration
     * (for pure composite activities) or implicitly defined by this activity (for semi-composite ones).
     */
    @NotNull
    protected abstract List<ActivityExecution> createChildren(OperationResult result) throws SchemaException;

    /** Executes child activities. */
    private void executeChildren(OperationResult result)
            throws TaskException, CommonException, PreconditionViolationException {
        for (ActivityExecution child : children) {
            executionResult = child.execute(result);
            applyErrorCriticality();
        }
        treatNullExecutionResult();
    }

    private void applyErrorCriticality() {
        LOGGER.warn("Error criticality checking is not implemented yet."); // TODO
    }

    private void treatNullExecutionResult() {
        if (executionResult == null) {
            executionResult = ActivityExecutionResult.finished();
        }
    }

    public @NotNull List<ActivityExecution> getChildren() {
        return children;
    }

    public void addChild(@NotNull ActivityExecution child) {
        children.add(child);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "activityDefinition=" + activityDefinition +
                ", children=" + children +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "children", children, indent+1);
        return sb.toString();
    }
}
