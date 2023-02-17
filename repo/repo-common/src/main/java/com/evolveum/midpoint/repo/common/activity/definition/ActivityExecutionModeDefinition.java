/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.Objects;
import java.util.function.Supplier;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.util.ConfigurationSpecificationTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;

import org.jetbrains.annotations.Nullable;

/**
 * Defines "execution mode" aspects of an activity: production/preview/dry-run/... plus additional information.
 *
 * TODO better name
 *
 * Corresponds to {@link ActivityExecutionDefinitionType} - TODO better name for that one as well
 */
public class ActivityExecutionModeDefinition implements DebugDumpable, Cloneable {

    @NotNull private ExecutionModeType mode;

    /**
     * This bean is detached copy dedicated for this definition. It is therefore freely modifiable.
     */
    @NotNull private final ActivityExecutionDefinitionType bean;

    private ActivityExecutionModeDefinition(
            @NotNull ExecutionModeType mode,
            @NotNull ActivityExecutionDefinitionType bean) {
        this.mode = mode;
        this.bean = bean;
    }

    public static @NotNull ActivityExecutionModeDefinition create(
            @Nullable ActivityDefinitionType activityDefinitionBean,
            @NotNull Supplier<ExecutionModeType> defaultValueSupplier) {
        if (activityDefinitionBean == null) {
            return new ActivityExecutionModeDefinition(defaultValueSupplier.get(), new ActivityExecutionDefinitionType());
        }
        ExecutionModeType mode = Objects.requireNonNullElseGet(activityDefinitionBean.getExecutionMode(), defaultValueSupplier);
        ActivityExecutionDefinitionType executionBean = activityDefinitionBean.getExecution();
        ActivityExecutionDefinitionType clonedBean =
                executionBean != null ? executionBean.clone() : new ActivityExecutionDefinitionType();
        return new ActivityExecutionModeDefinition(mode, clonedBean);
    }

    @Override
    public String toString() {
        return mode + "; " + bean.asPrismContainerValue().size() + " additional item(s)";
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump(indent);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityExecutionModeDefinition clone() {
        return new ActivityExecutionModeDefinition(mode, bean.clone());
    }

    public @NotNull ExecutionModeType getMode() {
        return mode;
    }

    public void setMode(@NotNull ExecutionModeType mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    void applyChangeTailoring(ActivityTailoringType tailoring) {
        ExecutionModeType tailoredMode = tailoring.getExecutionMode();
        if (tailoredMode != null) {
            mode = tailoredMode;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean shouldCreateSimulationResult() {
        Boolean explicitValue = bean.isCreateSimulationResult();
        if (explicitValue != null) {
            return explicitValue;
        }
        return bean.getSimulationDefinition() != null;
    }

    public SimulationDefinitionType getSimulationDefinition() {
        return bean.getSimulationDefinition();
    }

    public @Nullable ConfigurationSpecificationType getConfigurationSpecification() {
        return bean.getConfigurationToUse();
    }

    public TaskExecutionMode getTaskExecutionMode() throws ConfigurationException {
        switch (mode) {
            case FULL:
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.PRODUCTION;
                } else {
                    throw new ConfigurationException("Full execution mode requires the use of production configuration");
                }
            case PREVIEW:
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.SIMULATED_PRODUCTION;
                } else {
                    return TaskExecutionMode.SIMULATED_DEVELOPMENT;
                }
            case SHADOW_MANAGEMENT_PREVIEW:
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.SIMULATED_SHADOWS_PRODUCTION;
                } else {
                    return TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT;
                }
            case DRY_RUN:
            case NONE:
            case BUCKET_ANALYSIS:
                // These modes are treated in a special way - will be probably changed later
                // It is also unclear whether to insist on production configuration here.
                return TaskExecutionMode.PRODUCTION;
            default:
                throw new AssertionError(mode);

        }
    }

    private boolean isProductionConfiguration() {
        return ConfigurationSpecificationTypeUtil.isProductionConfiguration(bean.getConfigurationToUse());
    }
}