/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task.handlers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.task.execution.ActivityContext;
import com.evolveum.midpoint.repo.common.task.definition.CompositeWorkDefinition;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecution;

import com.evolveum.midpoint.repo.common.task.execution.PureCompositeActivityExecution;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCompositionType;

/**
 * TODO
 */
@Component
public class PureCompositionActivityHandler implements ActivityHandler<CompositeWorkDefinition> {

    @Autowired ActivityHandlerRegistry registry;

    @PostConstruct
    public void register() {
        registry.register(ActivityCompositionType.COMPLEX_TYPE, this);
    }

    @PreDestroy
    public void unregister() {
        registry.unregister(ActivityCompositionType.COMPLEX_TYPE);
    }

    @Override
    public @NotNull ActivityExecution createExecution(@NotNull ActivityContext<CompositeWorkDefinition> context,
            @NotNull OperationResult result) {
        return new PureCompositeActivityExecution(context);
    }
}
