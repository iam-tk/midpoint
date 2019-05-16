/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import java.util.List;

/**
 * Listener that accepts events generated by workflow module. These events are related to processes and work items.
 *
 * TODO what about tasks? Should the task (wfTask) be passed to the notification module?
 *
 * @author mederly
 */
@Component
public class WorkflowListener implements ProcessListener, WorkItemListener {

    private static final Trace LOGGER = TraceManager.getTrace(WorkflowListener.class);

    //private static final String DOT_CLASS = WorkflowListener.class.getName() + ".";

    @Autowired private NotificationManager notificationManager;
    @Autowired private NotificationFunctionsImpl functions;
    @Autowired private LightweightIdentifierGenerator identifierGenerator;

    // WorkflowManager is not required, because e.g. within model-test and model-intest we have no workflows.
    // However, during normal operation, it is expected to be available.

    @Autowired(required = false) private WorkflowManager workflowManager;

    @PostConstruct
    public void init() {
        if (workflowManager != null) {
            workflowManager.registerProcessListener(this);
            workflowManager.registerWorkItemListener(this);
        } else {
            LOGGER.warn("WorkflowManager not present, notifications for workflows will not be enabled.");
        }
    }

    //region Process-level notifications
    @Override
    public void onProcessInstanceStart(CaseType aCase, Task opTask,
		    OperationResult result) {
        WorkflowProcessEvent event = new WorkflowProcessEvent(identifierGenerator, ChangeType.ADD, aCase);
        initializeWorkflowEvent(event, aCase, opTask);
        processEvent(event, result);
    }

	@Override
	public void onProcessInstanceEnd(CaseType aCase, Task opTask,
			OperationResult result) {
		WorkflowProcessEvent event = new WorkflowProcessEvent(identifierGenerator, ChangeType.DELETE, aCase);
		initializeWorkflowEvent(event, aCase, opTask);
		processEvent(event, result);
	}
	//endregion

	//region WorkItem-level notifications
    @Override
    public void onWorkItemCreation(ObjectReferenceType assignee, @NotNull CaseWorkItemType workItem,
		    CaseType aCase, Task wfTask, OperationResult result) {
	    WorkItemEvent event = new WorkItemLifecycleEvent(identifierGenerator, ChangeType.ADD, workItem,
				SimpleObjectRefImpl.create(functions, assignee), null, null, null,
			    aCase.getWorkflowContext(), aCase);
		initializeWorkflowEvent(event, aCase, wfTask);
        processEvent(event, result);
    }

    @Override
    public void onWorkItemDeletion(ObjectReferenceType assignee, @NotNull CaseWorkItemType workItem,
		    WorkItemOperationInfo operationInfo, WorkItemOperationSourceInfo sourceInfo,
		    CaseType aCase, Task opTask, OperationResult result) {
	    WorkItemEvent event = new WorkItemLifecycleEvent(identifierGenerator, ChangeType.DELETE, workItem,
				SimpleObjectRefImpl.create(functions, assignee),
				getInitiator(sourceInfo), operationInfo, sourceInfo, aCase.getWorkflowContext(), aCase);
		initializeWorkflowEvent(event, aCase, opTask);
		processEvent(event, result);
    }

    @Override
    public void onWorkItemCustomEvent(ObjectReferenceType assignee, @NotNull CaseWorkItemType workItem,
		    @NotNull WorkItemNotificationActionType notificationAction, WorkItemEventCauseInformationType cause,
		    CaseType aCase, Task opTask, OperationResult result) {
	    WorkItemEvent event = new WorkItemCustomEvent(identifierGenerator, ChangeType.ADD, workItem,
				SimpleObjectRefImpl.create(functions, assignee),
				new WorkItemOperationSourceInfo(null, cause, notificationAction),
				aCase.getWorkflowContext(), aCase, notificationAction.getHandler());
		initializeWorkflowEvent(event, aCase, opTask);
		processEvent(event, result);
    }

    @Override
	public void onWorkItemAllocationChangeCurrentActors(@NotNull CaseWorkItemType workItem,
		    @NotNull WorkItemAllocationChangeOperationInfo operationInfo,
		    @Nullable WorkItemOperationSourceInfo sourceInfo,
		    Duration timeBefore, CaseType aCase, Task task,
		    OperationResult result) {
    	checkOids(operationInfo.getCurrentActors());
		for (ObjectReferenceType currentActor : operationInfo.getCurrentActors()) {
			onWorkItemAllocationModifyDelete(currentActor, workItem, operationInfo, sourceInfo, timeBefore, aCase, task, result);
		}
	}

	@Override
	public void onWorkItemAllocationChangeNewActors(@NotNull CaseWorkItemType workItem,
			@NotNull WorkItemAllocationChangeOperationInfo operationInfo,
			@Nullable WorkItemOperationSourceInfo sourceInfo,
			CaseType aCase, Task task, OperationResult result) {
    	Validate.notNull(operationInfo.getNewActors());

    	checkOids(operationInfo.getCurrentActors());
    	checkOids(operationInfo.getNewActors());
		for (ObjectReferenceType newActor : operationInfo.getNewActors()) {
			onWorkItemAllocationAdd(newActor, workItem, operationInfo, sourceInfo, aCase, task, result);
		}
	}

	private void checkOids(List<ObjectReferenceType> refs) {
		refs.forEach(r -> Validate.notNull(r.getOid(), "No OID in actor object reference " + r));
	}

	private void onWorkItemAllocationAdd(ObjectReferenceType newActor, @NotNull CaseWorkItemType workItem,
			@Nullable WorkItemOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
			CaseType aCase, Task task, OperationResult result) {
    	WorkItemAllocationEvent event = new WorkItemAllocationEvent(identifierGenerator, ChangeType.ADD, workItem,
				SimpleObjectRefImpl.create(functions, newActor),
				getInitiator(sourceInfo), operationInfo, sourceInfo,
				aCase.getWorkflowContext(), aCase, null);
    	initializeWorkflowEvent(event, aCase, task);
    	processEvent(event, result);
	}

	private SimpleObjectRef getInitiator(WorkItemOperationSourceInfo sourceInfo) {
		return sourceInfo != null ?
				SimpleObjectRefImpl.create(functions, sourceInfo.getInitiatorRef()) : null;
	}

	private void onWorkItemAllocationModifyDelete(ObjectReferenceType currentActor, @NotNull CaseWorkItemType workItem,
			@Nullable WorkItemOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
			Duration timeBefore, CaseType aCase, Task task,
			OperationResult result) {
		WorkItemAllocationEvent event = new WorkItemAllocationEvent(identifierGenerator,
				timeBefore != null ? ChangeType.MODIFY : ChangeType.DELETE, workItem,
				SimpleObjectRefImpl.create(functions, currentActor),
				getInitiator(sourceInfo), operationInfo, sourceInfo,
				aCase.getWorkflowContext(), aCase, timeBefore);
		initializeWorkflowEvent(event, aCase, task);
		processEvent(event, result);
	}
	//endregion

	private void processEvent(WorkflowEvent event, OperationResult result) {
        try {
            notificationManager.processEvent(event);
        } catch (RuntimeException e) {
            result.recordFatalError("An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
        }

        // todo work correctly with operationResult (in whole notification module)
        if (result.isUnknown()) {
            result.computeStatus();
        }
        result.recordSuccessIfUnknown();
    }

	private void initializeWorkflowEvent(WorkflowEvent event, CaseType aCase, Task wfTask) {
		event.setRequester(SimpleObjectRefImpl.create(functions, aCase.getRequestorRef()));
		event.setRequestee(SimpleObjectRefImpl.create(functions, aCase.getObjectRef()));
		// TODO what if requestee is yet to be created?
	}

}
