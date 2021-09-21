/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.evolveum.midpoint.gui.impl.page.admin.ProgressAwareChangesExecutorImpl;

import com.evolveum.midpoint.gui.impl.page.admin.component.ProgressReportingAwarePage;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangeExecutor;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.FocusOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.ProgressPanel;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class PageFocusDetails<F extends FocusType, FDM extends FocusDetailsModels<F>> extends PageAssignmentHolderDetails<F, FDM> implements ProgressReportingAwarePage {

    private static final Trace LOGGER = TraceManager.getTrace(PageFocusDetails.class);

    private static final String ID_PROGRESS_PANEL_FRAGMENT = "progressPanelFragment";
    private static final String ID_PROGRESS_PANEL = "progressPanel";

    private boolean saveOnConfigure;
    protected boolean previewRequested;

    public PageFocusDetails() {
        super();
    }

    public PageFocusDetails(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageFocusDetails(PrismObject<F> focus) {
        super(focus);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (saveOnConfigure) {
            saveOnConfigure = false;
            add(new AbstractAjaxTimerBehavior(Duration.ofMillis(100)) {
                @Override
                protected void onTimer(AjaxRequestTarget target) {
                    stop(target);
                    savePerformed(target);
                }
            });
        }
    }

    public void setSaveOnConfigure(boolean saveOnConfigure) {
        this.saveOnConfigure = saveOnConfigure;
    }

    @Override
    protected FocusOperationalButtonsPanel<F> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<F>> wrapperModel) {
        return new FocusOperationalButtonsPanel<>(id, wrapperModel) {

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageFocusDetails.this.savePerformed(target);
            }

            @Override
            protected void previewPerformed(AjaxRequestTarget target) {
                PageFocusDetails.this.previewPerformed(target);
            }

        };
    }

    @Override
    public void savePerformed(AjaxRequestTarget target) {
        previewRequested = false;
        super.savePerformed(target);
    }

    public void previewPerformed(AjaxRequestTarget target) {
        previewRequested = true;
        OperationResult result = new OperationResult(OPERATION_PREVIEW_CHANGES);
        saveOrPreviewPerformed(target, result, true);
    }

    class ProgressFragment extends Fragment {

        private final ExecuteChangeOptionsDto options;

        public ProgressFragment(String id, String markupId, MarkupContainer markupProvider, ExecuteChangeOptionsDto options) {
            super(id, markupId, markupProvider);
            this.options = options;
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            ProgressPanel progressPanel = new ProgressPanel(ID_PROGRESS_PANEL, options, PageFocusDetails.this);
            add(progressPanel);
        }

        public ProgressPanel getFragmentProgressPanel() {
            return (ProgressPanel) get(ID_PROGRESS_PANEL);
        }
    }

    @Override
    protected Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly, ExecuteChangeOptionsDto options, Task task, OperationResult result, AjaxRequestTarget target) {
        try {
            if (options.isReconcile() && deltas.isEmpty()) {
                ObjectDelta emptyDelta = getPrismContext().deltaFor(getType()).asObjectDelta(getModelPrismObject().getOid());
                deltas.add(emptyDelta);
            }
        } catch (SchemaException e) {
            LOGGER.error("Cannot crate empty delta, {}", e.getMessage(), e);
            target.add(getFeedbackPanel());
            return null;
        }

        return super.executeChanges(deltas, previewOnly, options, task, result, target);
    }

//    @Override
//    protected final void saveOrPreviewPostProcess(AjaxRequestTarget target, Task task, ExecuteChangeOptionsDto options) {
//        Fragment progressPanelFragment = new ProgressFragment(ID_DETAILS_VIEW, ID_PROGRESS_PANEL_FRAGMENT, PageFocusDetails.this, options);
//        replace(progressPanelFragment);
//        target.add(progressPanelFragment);
//    }

//    @Override
//    protected void postProcessDeltas(AjaxRequestTarget target, Collection<ObjectDelta<? extends ObjectType>> deltas, ItemStatus status, ExecuteChangeOptionsDto executeChangeOptionsDto) throws SchemaException {
//        super.postProcessDeltas(target, deltas, status, executeChangeOptionsDto);
//
//        if (executeChangeOptionsDto.isReconcile() && deltas.isEmpty()) {
//            ObjectDelta emptyDelta = getPrismContext().deltaFor(getType()).asObjectDelta(getModelPrismObject().getOid());
//            deltas.add(emptyDelta);
//        }
//    }

    protected ProgressPanel getProgressPanel() {
        return (ProgressPanel) get(createComponentPath(ID_DETAILS_VIEW, ID_PROGRESS_PANEL));
    }

    @Override
    protected ObjectChangeExecutor getChangeExecutor() {
        return new ProgressAwareChangesExecutorImpl(getExecuteChangesOptionsDto(), this);
    }

    @Override
    protected FDM createObjectDetailsModels(PrismObject<F> object) {
        return (FDM) new FocusDetailsModels<>(createPrismObejctModel(object), this);
    }


    @Override
    public ProgressPanel startAndGetProgressPanel(AjaxRequestTarget target, OperationResult result) {
        LOGGER.trace("startProcessing called, making main panel invisible");
        ProgressFragment progressPanelFragment = new ProgressFragment(ID_DETAILS_VIEW, ID_PROGRESS_PANEL_FRAGMENT, PageFocusDetails.this, getExecuteChangesOptionsDto());
        replace(progressPanelFragment);
        target.add(progressPanelFragment);
        return progressPanelFragment.getFragmentProgressPanel();
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, boolean returningFromAsync, OperationResult result) {
        if (previewRequested) {
            finishPreviewProcessing(target, result);
            return;
        }

        setTimezoneIfNeeded(result);

        boolean focusAddAttempted = getDelta() != null && getDelta().isAdd();
        boolean focusAddSucceeded = focusAddAttempted && StringUtils.isNotEmpty(getDelta().getOid());

        // we don't want to allow resuming editing if a new focal object was created (on second 'save' there would be a conflict with itself)
        // and also in case of partial errors, like those related to projections (many deltas would be already executed, and this could cause problems on second 'save').
        boolean canContinueEditing = !focusAddSucceeded && result.isFatalError();

        boolean canExitPage;
        if (returningFromAsync) {
            canExitPage = getProgressPanel().isAllSuccess() || result.isInProgress() || result.isHandledError(); // if there's at least a warning in the progress table, we would like to keep the table open
        } else {
            canExitPage = !canContinueEditing;                            // no point in staying on page if we cannot continue editing (in synchronous case i.e. no progress table present)
        }

        if (!getProgressPanel().isKeepDisplayingResults() && canExitPage) {
            showResult(result);
            redirectBack();
        } else {
            getProgressPanel().manageButtons(target, returningFromAsync, canContinueEditing);
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    private void setTimezoneIfNeeded(OperationResult result) {
        if (result.isSuccess() && getDelta() != null && SecurityUtils.getPrincipalUser().getOid().equals(getDelta().getOid())) {
            Session.get().setLocale(WebComponentUtil.getLocale());
            LOGGER.debug("Using {} as locale", getLocale());
            WebSession.get().getClientInfo().getProperties().
                    setTimeZone(WebModelServiceUtils.getTimezone());
            LOGGER.debug("Using {} as time zone", WebSession.get().getClientInfo().getProperties().getTimeZone());
        }
    }

    private ObjectDelta<F> getDelta() {
        return getObjectDetailsModels().getDelta();
    }

    private void finishPreviewProcessing(AjaxRequestTarget target, OperationResult result) {
        showResult(result);
        target.add(getFeedbackPanel());

        Map<PrismObject<F>, ModelContext<? extends ObjectType>> modelContextMap = new LinkedHashMap<>();
        collectObjectsForPreview(modelContextMap);

        DetailsFragment detailsFragment = createDetailsFragment();
        replace(detailsFragment);
        target.add(detailsFragment);
        navigateToNext(new PageFocusPreviewChanges(modelContextMap));
    }

    protected void collectObjectsForPreview(Map<PrismObject<F>, ModelContext<? extends ObjectType>> modelContextMap) {
        modelContextMap.put(getModelPrismObject(), getProgressPanel().getPreviewResult());
    }


    @Override
    public void continueEditing(AjaxRequestTarget target) {
        DetailsFragment detailsFragment = createDetailsFragment();
        replace(detailsFragment);
        target.add(detailsFragment);
    }
}
