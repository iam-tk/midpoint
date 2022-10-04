/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation.CorrelationItemRefsTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation.CorrelationItemsTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */

@Experimental
public class AssociationsWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public AssociationsWizardPanel(String id, ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected AssociationsTableWizardPanel createTablePanel() {
        AssociationsTableWizardPanel table =
                new AssociationsTableWizardPanel(getIdOfChoicePanel(), getResourceModel(), valueModel) {

                    @Override
                    protected void onSaveResourcePerformed(AjaxRequestTarget target) {
                        if (!isSavedAfterWizard()) {
                            onExitPerformed(target);
                            return;
                        }
                        OperationResult result = AssociationsWizardPanel.this.onSaveResourcePerformed(target);
                        if (result != null && !result.isError()) {
                            new Toast()
                                    .success()
                                    .title(getString("ResourceWizardPanel.updateResource"))
                                    .icon("fas fa-circle-check")
                                    .autohide(true)
                                    .delay(5_000)
                                    .body(getString("ResourceWizardPanel.updateResource.text")).show(target);
                            onExitPerformed(target);
                        }
                    }

                    @Override
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        AssociationsWizardPanel.this.onExitPerformed(target);
                    }

                    @Override
                    protected IModel<String> getSubmitLabelModel() {
                        if (isSavedAfterWizard()) {
                            return super.getSubmitLabelModel();
                        }
                        return getPageBase().createStringResource("WizardPanel.confirm");
                    }

                    @Override
                    protected void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> value, AjaxRequestTarget target) {
                        showWizardFragment(target, new WizardPanel(
                                getIdOfWizardPanel(),
                                new WizardModel(createAssociationsSteps(value))));
                    }

                    @Override
                    protected String getSubmitIcon() {
                        if (isSavedAfterWizard()) {
                            return super.getSubmitIcon();
                        }
                        return "fa fa-check";
                    }
                };
        return table;
    }

    public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    private List<WizardStep> createAssociationsSteps(IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        AssociationStepPanel panel = new AssociationStepPanel(
                getResourceModel(),
                valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        };
        panel.setOutputMarkupId(true);
        steps.add(panel);

        return steps;
    }

    protected boolean isSavedAfterWizard() {
        return true;
    }
}