/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;


/**
 * @author lskublik
 */
public class BasicWizardPanel<T> extends WizardStepPanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";
    private static final String ID_BACK = "back";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_LABEL = "nextLabel";

    public BasicWizardPanel() {
    }

    public BasicWizardPanel(IModel<T> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Label mainText = new Label(ID_TEXT, getTextModel());
        mainText.add(new VisibleBehaviour(() -> getTextModel().getObject() != null));
        add(mainText);

        Label secondaryText = new Label(ID_SUBTEXT, getSubTextModel());
        secondaryText.add(new VisibleBehaviour(() -> getSubTextModel().getObject() != null));
        add(secondaryText);

        AjaxLink back = new AjaxLink<>(ID_BACK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        back.add(getBackBehaviour());
        back.setOutputMarkupId(true);
        back.setOutputMarkupPlaceholderTag(true);
        back.add(AttributeAppender.append("class", () -> !back.isEnabledInHierarchy() ? "disabled" : null));
        add(back);

        AjaxSubmitButton next = new AjaxSubmitButton(ID_NEXT) {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onNextPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                updateFeedbackPanels(target);
            }
        };
        next.add(getNextBehaviour());
        next.setOutputMarkupId(true);
        next.setOutputMarkupPlaceholderTag(true);
        next.add(AttributeAppender.append("class", () -> !next.isEnabledInHierarchy() ? "disabled" : null));
        add(next);

        Label nextLabel = new Label(ID_NEXT_LABEL, () -> {
            WizardStep step = getWizard().getNextPanel();
            return step != null ? step.getTitle().getObject() : null;
        });
        next.add(nextLabel);
    }

    protected void updateFeedbackPanels(AjaxRequestTarget target) {
    }

    protected WebMarkupContainer createContentPanel(String id) {
        return new WebMarkupContainer(id);
    }

    protected AjaxLink getNext() {
        return (AjaxLink) get(ID_NEXT);
    }

    protected AjaxLink getBack() {
        return (AjaxLink) get(ID_BACK);
    }

    protected IModel<String> getTextModel() {
        return Model.of();
    }

    protected IModel<String> getSubTextModel() {
        return Model.of();
    }

    protected void onNextPerformed(AjaxRequestTarget target) {
        getWizard().next();
        target.add(getWizard().getPanel());
    }

    protected void onBackPerformed(AjaxRequestTarget target) {
        int index = getWizard().getActiveStepIndex();
        if (index > 0) {
            getWizard().previous();
            target.add(getWizard().getPanel());
            return;
        }
        onBackAfterWizardPerformed(target);
    }

    // todo why is this needed? please remove and use onBackPerformed(AjaxRequestTarget)
    @Deprecated
    protected void onBackAfterWizardPerformed(AjaxRequestTarget target) {
        getPageBase().redirectBack();
    }

    @Override
    public VisibleEnableBehaviour getHeaderBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}