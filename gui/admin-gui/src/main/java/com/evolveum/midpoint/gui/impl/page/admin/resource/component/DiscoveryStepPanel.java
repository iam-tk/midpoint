/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.verticalForm.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.verticalForm.VerticalFormPrismPropertyValuePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
@PanelType(name = "discoverWizard")
@PanelInstance(identifier = "discoverWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        defaultPanel = true,
        display = @PanelDisplay(label = "PageResource.wizard.step.discovery", icon = "fa fa-list-check"),
        containerPath = "connectorConfiguration/configurationProperties",
        expanded = true)
public class DiscoveryStepPanel extends AbstractResourceWizardStepPanel {

    private static final String PANEL_TYPE = "discoverWizard";

    public DiscoveryStepPanel(ResourceDetailsModel model) {
        super(model);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-list-check";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.discovery");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.discovery.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.discovery.subText");
    }

    protected ItemVisibility checkVisibility(ItemWrapper itemWrapper) {
        if(itemWrapper.isMandatory()) {
            return ItemVisibility.HIDDEN;
        }
        return ItemVisibility.AUTO;
    }
}