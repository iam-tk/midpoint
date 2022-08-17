/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.model.api.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Controller
public class AdminGuiConfigurationMergeManagerImpl implements AdminGuiConfigurationMergeManager {

    private static final Trace LOGGER = TraceManager.getTrace(AdminGuiConfigurationMergeManagerImpl.class);

    @Override
    public List<ContainerPanelConfigurationType> mergeContainerPanelConfigurationType(List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> configuredPanels) {
        List<ContainerPanelConfigurationType> mergedPanels = new ArrayList<>(defaultPanels);
        for (ContainerPanelConfigurationType configuredPanel : configuredPanels) {
            mergePanelConfigurations(configuredPanel, defaultPanels, mergedPanels);
        }
        MiscSchemaUtil.sortDetailsPanels(mergedPanels);
        return mergedPanels;
    }

    @Override
    public GuiObjectDetailsPageType mergeObjectDetailsPageConfiguration(@NotNull GuiObjectDetailsPageType defaultPageConfiguration, ArchetypePolicyType archetypePolicyType, OperationResult result) throws SchemaException, ConfigurationException {
        return mergeObjectDetailsPageConfiguration(defaultPageConfiguration, archetypePolicyType);
    }

    private GuiObjectDetailsPageType mergeObjectDetailsPageConfiguration(GuiObjectDetailsPageType defaultPageConfiguration, ArchetypePolicyType archetypePolicyType) {
        if (archetypePolicyType == null) {
            return defaultPageConfiguration;
        }

        ArchetypeAdminGuiConfigurationType archetypeAdminGuiConfigurationType = archetypePolicyType.getAdminGuiConfiguration();
        if (archetypeAdminGuiConfigurationType == null) {
            return defaultPageConfiguration;
        }

        GuiObjectDetailsPageType archetypePageConfiguration = archetypeAdminGuiConfigurationType.getObjectDetails();
        if (archetypePageConfiguration == null) {
            return defaultPageConfiguration;
        }

        return mergeObjectDetailsPageConfiguration(defaultPageConfiguration, archetypePageConfiguration);
    }

    @Override
    public GuiObjectDetailsPageType mergeObjectDetailsPageConfiguration(GuiObjectDetailsPageType defaultPageConfiguration, GuiObjectDetailsPageType compiledPageType) {
        GuiObjectDetailsPageType mergedDetailsPage = defaultPageConfiguration.cloneWithoutId();

        return mergeDetailsPages(defaultPageConfiguration, compiledPageType, mergedDetailsPage);
    }

    private <DP extends GuiObjectDetailsPageType> DP mergeDetailsPages(DP defaultPageConfiguration, DP compiledPageType, DP mergedDetailsPage) {
        if (compiledPageType == null) {
            //just to be sure that everything is correctly sorted
            return defaultPageConfiguration;
        }

        List<ContainerPanelConfigurationType> mergedPanels = mergeContainerPanelConfigurationType(mergedDetailsPage.getPanel(), compiledPageType.getPanel());

        //backward compatibility
        Optional<ContainerPanelConfigurationType> optionalPropertiesPanelConfiguration = mergedPanels
                .stream()
                .filter(p -> containsConfigurationForEmptyPath(p)).findFirst();

        if (optionalPropertiesPanelConfiguration.isPresent()) {
            ContainerPanelConfigurationType propertiesPanelConfiguration = optionalPropertiesPanelConfiguration.get();
            List<VirtualContainersSpecificationType> virtualContainers = mergeVirtualContainers(propertiesPanelConfiguration.getContainer(), mergedDetailsPage.getContainer());
            propertiesPanelConfiguration.getContainer().clear();
            propertiesPanelConfiguration.getContainer().addAll(CloneUtil.cloneCollectionMembersWithoutIds(virtualContainers));

            virtualContainers = mergeVirtualContainers(propertiesPanelConfiguration.getContainer(), compiledPageType.getContainer());
            MiscSchemaUtil.sortFeaturesPanels(virtualContainers);
            propertiesPanelConfiguration.getContainer().clear();
            propertiesPanelConfiguration.getContainer().addAll(CloneUtil.cloneCollectionMembersWithoutIds(virtualContainers));
        }

        mergedDetailsPage.getPanel().clear();
        mergedDetailsPage.getPanel().addAll(CloneUtil.cloneCollectionMembersWithoutIds(mergedPanels));

        SummaryPanelSpecificationType mergedSummaryPanel = mergeSummaryPanels(mergedDetailsPage.getSummaryPanel(), compiledPageType.getSummaryPanel());
        mergedDetailsPage.setSummaryPanel(mergedSummaryPanel);

        if (compiledPageType.getSaveMethod() != null) {
            mergedDetailsPage.saveMethod(compiledPageType.getSaveMethod());
        }

        if (mergedDetailsPage.getForms() == null) {
            mergedDetailsPage.forms(compiledPageType.getForms());
        } else if (compiledPageType.getForms() != null) {
            mergeFormObject(mergedDetailsPage.getForms(), compiledPageType.getForms());
        }

        if (mergedDetailsPage.getRoleRelation() == null) {
            mergedDetailsPage.roleRelation(mergedDetailsPage.getRoleRelation());
        } else if (mergedDetailsPage.getRoleRelation() != null) {
            mergeRoleRelation(mergedDetailsPage.getRoleRelation(), mergedDetailsPage.getRoleRelation());
        }

        return mergedDetailsPage;
    }

    private static void mergeRoleRelation(RoleRelationObjectSpecificationType currentRoleRelation, RoleRelationObjectSpecificationType newRoleRelation) {
        if (newRoleRelation.getObjectRelation() != null) {
            currentRoleRelation.objectRelation(newRoleRelation.getObjectRelation());
        }

        if (!newRoleRelation.getSubjectRelation().isEmpty()) {
            newRoleRelation.getSubjectRelation().forEach(
                    newSubject -> currentRoleRelation.getSubjectRelation().removeIf(
                            currentSubject -> QNameUtil.match(currentSubject, newSubject)
                    )
            );
            currentRoleRelation.getSubjectRelation().addAll(newRoleRelation.getSubjectRelation());
        }

        if (newRoleRelation.isIncludeMembers() != null) {
            currentRoleRelation.includeMembers(newRoleRelation.isIncludeMembers());
        }

        if (newRoleRelation.isIncludeReferenceRole() != null) {
            currentRoleRelation.includeReferenceRole(newRoleRelation.isIncludeReferenceRole());
        }
    }

    private static void mergeFormObject(ObjectFormType currentForm, ObjectFormType newForm) {
        if (newForm.getFormSpecification() != null) {
            currentForm.formSpecification(newForm.getFormSpecification());
        }

        if (newForm.isIncludeDefaultForms() != null) {
            currentForm.includeDefaultForms(newForm.isIncludeDefaultForms());
        }
    }

    private SummaryPanelSpecificationType mergeSummaryPanels(SummaryPanelSpecificationType defaultSummary, SummaryPanelSpecificationType compiledSummary) {
        if (compiledSummary == null) {
            return defaultSummary;
        }

        if (defaultSummary == null) {
            return compiledSummary;
        }

        SummaryPanelSpecificationType mergedSummary = defaultSummary.cloneWithoutId();
        GuiFlexibleLabelType mergedIdentifier = mergeSummaryPanelFlexibleLabel(defaultSummary.getIdentifier(), compiledSummary.getIdentifier());
        mergedSummary.setIdentifier(mergedIdentifier);

        GuiFlexibleLabelType mergedDisplayName = mergeSummaryPanelFlexibleLabel(defaultSummary.getDisplayName(), compiledSummary.getDisplayName());
        mergedSummary.setDisplayName(mergedDisplayName);

        GuiFlexibleLabelType mergedOrganization = mergeSummaryPanelFlexibleLabel(defaultSummary.getOrganization(), compiledSummary.getOrganization());
        mergedSummary.setOrganization(mergedOrganization);

        GuiFlexibleLabelType mergedTitle1 = mergeSummaryPanelFlexibleLabel(defaultSummary.getTitle1(), compiledSummary.getTitle1());
        mergedSummary.setTitle1(mergedTitle1);

        GuiFlexibleLabelType mergedTitle2 = mergeSummaryPanelFlexibleLabel(defaultSummary.getTitle2(), compiledSummary.getTitle2());
        mergedSummary.setTitle2(mergedTitle2);

        GuiFlexibleLabelType mergedTitle3 = mergeSummaryPanelFlexibleLabel(defaultSummary.getTitle3(), compiledSummary.getTitle3());
        mergedSummary.setTitle3(mergedTitle3);

        return mergedSummary;
    }

    private GuiFlexibleLabelType mergeSummaryPanelFlexibleLabel(GuiFlexibleLabelType defaultSummaryPanelIdentifier, GuiFlexibleLabelType compiledSummaryPanelIdentifier) {
        if (compiledSummaryPanelIdentifier == null) {
            return defaultSummaryPanelIdentifier != null ? defaultSummaryPanelIdentifier.cloneWithoutId() : null;
        }

        GuiFlexibleLabelType mergedFlexibleLabel = defaultSummaryPanelIdentifier != null ?
                defaultSummaryPanelIdentifier.cloneWithoutId() : new GuiFlexibleLabelType();
        if (compiledSummaryPanelIdentifier.getVisibility() != null) {
            mergedFlexibleLabel.setVisibility(compiledSummaryPanelIdentifier.getVisibility());
        }

        if (compiledSummaryPanelIdentifier.getExpression() != null) {
            mergedFlexibleLabel.setExpression(compiledSummaryPanelIdentifier.getExpression());
        }

        return mergedFlexibleLabel;
    }

    @Override
    public GuiShadowDetailsPageType mergeShadowDetailsPageConfiguration(GuiShadowDetailsPageType defaultPageConfiguration, GuiShadowDetailsPageType compiledPageType) {
        GuiShadowDetailsPageType mergedDetailsPage = defaultPageConfiguration.cloneWithoutId();
        return mergeDetailsPages(defaultPageConfiguration, compiledPageType, mergedDetailsPage);
    }

    private boolean containsConfigurationForEmptyPath(ContainerPanelConfigurationType panel) {
        return panel.getContainer().stream()
                .filter(c -> pathsMatch(c.getPath(), new ItemPathType(ItemPath.EMPTY_PATH)))
                .findFirst()
                .isPresent();
    }

    private void mergePanelConfigurations(ContainerPanelConfigurationType configuredPanel, List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> mergedPanels) {
        for (ContainerPanelConfigurationType defaultPanel : defaultPanels) {
            if (StringUtils.isEmpty(defaultPanel.getIdentifier())) {
                LOGGER.trace("Unable to merge container panel configuration, identifier shouldn't be empty, {}", defaultPanel);
                continue;
            }
            if (defaultPanel.getIdentifier().equals(configuredPanel.getIdentifier())) {
                mergePanels(defaultPanel, configuredPanel);
                return;
            }
        }

        mergedPanels.add(configuredPanel.cloneWithoutId());
    }

    private void mergePanels(ContainerPanelConfigurationType mergedPanel, ContainerPanelConfigurationType configuredPanel) {
        if (configuredPanel.getPanelType() != null) {
            mergedPanel.setPanelType(configuredPanel.getPanelType());
        }

        DisplayType mergedDisplayType = mergeDisplayType(configuredPanel.getDisplay(), mergedPanel.getDisplay());
        mergedPanel.setDisplay(mergedDisplayType);

        if (configuredPanel.getDisplayOrder() != null) {
            mergedPanel.setDisplayOrder(configuredPanel.getDisplayOrder());
        }

        if (configuredPanel.getPath() != null) {
            mergedPanel.setPath(configuredPanel.getPath());
        }

        if (configuredPanel.getListView() != null) {
            mergedPanel.setListView(configuredPanel.getListView().cloneWithoutId());
        }

        if (!configuredPanel.getContainer().isEmpty()) {
            List<VirtualContainersSpecificationType> virtualContainers = mergeVirtualContainers(configuredPanel.getContainer(), mergedPanel.getContainer());
            mergedPanel.getContainer().clear();
            mergedPanel.getContainer().addAll(virtualContainers);
        }

        if (configuredPanel.getType() != null) {
            mergedPanel.setType(configuredPanel.getType());
        }

        if (configuredPanel.getVisibility() != null) {
            mergedPanel.setVisibility(configuredPanel.getVisibility());
        }

        if (configuredPanel.isDefault() != null) {
            mergedPanel.setDefault(configuredPanel.isDefault());
        }

        if (!configuredPanel.getPanel().isEmpty()) {
            List<ContainerPanelConfigurationType> mergedConfigs = mergeContainerPanelConfigurationType(mergedPanel.getPanel(), configuredPanel.getPanel());
            mergedPanel.getPanel().clear();
            mergedPanel.getPanel().addAll(mergedConfigs);
        }
        if (CollectionUtils.isNotEmpty(configuredPanel.getAction())) {
            if (mergedPanel.getAction() == null) {
                mergedPanel.createActionList().addAll(configuredPanel.getAction());
            } else if (mergedPanel.getAction().isEmpty()) {
                mergedPanel.getAction().addAll(configuredPanel.getAction());
            } else {
                List<GuiActionType> mergedActions = mergeGuiActions(mergedPanel.getAction(), configuredPanel.getAction());
                mergedPanel.getAction().clear();
                mergedPanel.getAction().addAll(mergedActions);
            }
        }
    }

    private List<GuiActionType> mergeGuiActions(List<GuiActionType> composited, List<GuiActionType> actionList) {
        List<GuiActionType> mergedList = new ArrayList<>();
        for (GuiActionType action : actionList) {
            GuiActionType actionInList = findAction(composited, action.getName());
            if (actionInList == null) {
                mergedList.add(actionInList);
            } else {
                mergeGuiAction(actionInList, action);
            }
        }
        return mergedList;
    }

    private GuiActionType findAction(List<GuiActionType> actionList, String actionName) {
        if (StringUtils.isEmpty(actionName)) {
            return null;
        }
        return actionList.stream().filter(action -> actionName.equals(action.getName())).findFirst().orElse(null);
    }

    private void mergeGuiAction(GuiActionType composited, GuiActionType action) {
        if (action == null) {
            return;
        }
        if (composited == null) {
            return;
        }
        if (StringUtils.isEmpty(composited.getName()) || StringUtils.isEmpty(action.getName())) {
            LOGGER.trace("Unable to merge gui action without name, action 1: {}, action 2: {}", composited, action);
        }
        if (StringUtils.isNotEmpty(action.getDescription())) {
            composited.setDocumentation(action.getDescription());
        }
        if (action.getDisplay() != null) {
            composited.setDisplay(mergeDisplayType(action.getDisplay(), composited.getDisplay()));
        }
        if (StringUtils.isNotEmpty(action.getDocumentation())) {
            composited.setDescription(action.getDocumentation());
        }
        if (action.getTaskTemplateRef() != null) {
            composited.setTaskTemplateRef(action.getTaskTemplateRef());
        }
        mergeRedirectionTargetType(composited.getTarget(), action.getTarget().clone());
    }

    private void mergeRedirectionTargetType(RedirectionTargetType composited, RedirectionTargetType redirectionTarget) {
        if (composited == null) {
            return;
        }
        if (redirectionTarget == null) {
            return;
        }

        if (StringUtils.isNotEmpty(redirectionTarget.getTargetUrl())) {
            composited.setTargetUrl(redirectionTarget.getTargetUrl());
        }
        if (StringUtils.isNotEmpty(redirectionTarget.getPanelType())) {
            composited.setPanelType(redirectionTarget.getPanelType());
        }
        if (StringUtils.isNotEmpty(redirectionTarget.getPageClass())) {
            composited.setPageClass(redirectionTarget.getPageClass());
        }
    }

    public List<VirtualContainersSpecificationType> mergeVirtualContainers(GuiObjectDetailsPageType currentObjectDetails, GuiObjectDetailsPageType superObjectDetails) {
        return mergeContainers(currentObjectDetails.getContainer(), superObjectDetails.getContainer(),
                this::createVirtualContainersPredicate, this::mergeVirtualContainer);
    }

    private List<VirtualContainersSpecificationType> mergeVirtualContainers(List<VirtualContainersSpecificationType> currentVirtualContainers, List<VirtualContainersSpecificationType> superObjectDetails) {
        return mergeContainers(currentVirtualContainers, superObjectDetails,
                this::createVirtualContainersPredicate, this::mergeVirtualContainer);
    }

    private Predicate<VirtualContainersSpecificationType> createVirtualContainersPredicate(VirtualContainersSpecificationType superContainer) {
        return c -> identifiersMatch(c.getIdentifier(), superContainer.getIdentifier()) || pathsMatch(superContainer.getPath(), c.getPath());
    }

    public <C extends Containerable> List<C> mergeContainers(List<C> currentContainers, List<C> superContainers, Function<C, Predicate<C>> predicate, BiFunction<C, C, C> mergeFunction) {
        if (currentContainers.isEmpty()) {
            if (superContainers.isEmpty()) {
                return Collections.emptyList();
            }
            return superContainers.stream().map(this::cloneComplex).collect(Collectors.toList());
        }

        if (superContainers.isEmpty()) {
            return currentContainers.stream().map(this::cloneComplex).collect(Collectors.toList());
        }

        List<C> mergedContainers = new ArrayList<>();
        for (C superContainer : superContainers) {
            C matchedContainer = find(predicate.apply(superContainer), currentContainers);
            if (matchedContainer != null) {
                C mergedContainer = mergeFunction.apply(matchedContainer, superContainer);
                mergedContainers.add(mergedContainer);
            } else {
                mergedContainers.add(cloneComplex(superContainer));
            }
        }

        for (C currentContainer : currentContainers) {
            if (!findAny(predicate.apply(currentContainer), mergedContainers)) {
                mergedContainers.add(cloneComplex(currentContainer));
            }
        }

        return mergedContainers;
    }

    private <C extends Containerable> C find(Predicate<C> predicate, List<C> currentContainers) {
        List<C> matchedContainers = currentContainers.stream()
                .filter(predicate)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(matchedContainers)) {
            return null;
        }

        if (matchedContainers.size() > 1) {
            throw new IllegalStateException("Cannot merge virtual containers. More containers with same identifier specified.");
        }

        return matchedContainers.iterator().next();
    }

    private <C extends Containerable> boolean findAny(Predicate<C> predicate, List<C> mergedContainers) {
        return mergedContainers.stream().anyMatch(predicate);
    }


    private boolean identifiersMatch(String id1, String id2) {
        return id1 != null && id1.equals(id2);
    }

    private VirtualContainersSpecificationType mergeVirtualContainer(VirtualContainersSpecificationType currentContainer, VirtualContainersSpecificationType superContainer) {
        VirtualContainersSpecificationType mergedContainer = currentContainer.clone();
        if (currentContainer.getDescription() == null) {
            mergedContainer.setDescription(superContainer.getDescription());
        }

        DisplayType mergedDisplayType = mergeDisplayType(currentContainer.getDisplay(), superContainer.getDisplay());
        mergedContainer.setDisplay(mergedDisplayType);

        if (currentContainer.getDisplayOrder() == null) {
            mergedContainer.setDisplayOrder(superContainer.getDisplayOrder());
        }

        if (currentContainer.getVisibility() == null) {
            mergedContainer.setVisibility(superContainer.getVisibility());
        }

        if (currentContainer.isExpanded() == null) {
            mergedContainer.setExpanded(superContainer.isExpanded());
        }

        for (VirtualContainerItemSpecificationType virtualItem : superContainer.getItem()) {
            if (currentContainer.getItem().stream().noneMatch(i -> pathsMatch(i.getPath(), virtualItem.getPath()))) {
                mergedContainer.getItem().add(cloneComplex(virtualItem));
            }
        }

        return mergedContainer;
    }

    private <C extends Containerable> C cloneComplex(C containerable) {
        return containerable.cloneWithoutId();
    }

    public DisplayType mergeDisplayType(DisplayType currentDisplayType, DisplayType superDisplayType) {
        if (currentDisplayType == null) {
            if (superDisplayType == null) {
                return null;
            }
            return superDisplayType.clone();
        }

        if (superDisplayType == null) {
            return currentDisplayType.clone();
        }

        DisplayType mergedDisplayType = currentDisplayType.clone();
        if (currentDisplayType.getLabel() == null) {
            mergedDisplayType.setLabel(superDisplayType.getLabel());
        }

        if (currentDisplayType.getColor() == null) {
            mergedDisplayType.setColor(superDisplayType.getColor());
        }

        if (currentDisplayType.getCssClass() == null) {
            mergedDisplayType.setCssClass(superDisplayType.getCssClass());
        }

        if (currentDisplayType.getCssStyle() == null) {
            mergedDisplayType.setCssStyle(superDisplayType.getCssStyle());
        }

        if (currentDisplayType.getHelp() == null) {
            mergedDisplayType.setHelp(superDisplayType.getHelp());
        }

        IconType mergedIcon = mergeIcon(currentDisplayType.getIcon(), superDisplayType.getIcon());
        mergedDisplayType.setIcon(mergedIcon);

        if (currentDisplayType.getPluralLabel() == null) {
            mergedDisplayType.setPluralLabel(superDisplayType.getPluralLabel());
        }

        if (currentDisplayType.getSingularLabel() == null) {
            mergedDisplayType.setSingularLabel(superDisplayType.getSingularLabel());
        }

        if (currentDisplayType.getTooltip() == null) {
            mergedDisplayType.setTooltip(superDisplayType.getTooltip());
        }

        return mergedDisplayType;
    }

    private IconType mergeIcon(IconType currentIcon, IconType superIcon) {
        if (currentIcon == null) {
            if (superIcon == null) {
                return null;
            }
            return superIcon.clone();
        }

        if (superIcon == null) {
            return currentIcon.clone();
        }

        IconType mergedIcon = currentIcon.clone();
        if (currentIcon.getCssClass() == null) {
            mergedIcon.setCssClass(superIcon.getCssClass());
        }

        if (currentIcon.getColor() == null) {
            mergedIcon.setColor(superIcon.getColor());
        }

        if (currentIcon.getImageUrl() == null) {
            mergedIcon.setImageUrl(superIcon.getImageUrl());
        }

        return mergedIcon;
    }

    private boolean pathsMatch(ItemPathType supperPath, ItemPathType currentPath) {
        return supperPath != null && currentPath != null && supperPath.equivalent(currentPath);
    }
}
