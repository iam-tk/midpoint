/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationPanel;
import com.evolveum.midpoint.web.component.prism.show.WrapperVisualization;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/object/${CONTAINER_ID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*/object/?*"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/mark/${MARK_OID}/object/${CONTAINER_ID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*/mark/?*/object/?*")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_PROCESSED_OBJECT_URL,
                        label = "PageSimulationResultObject.auth.simulationProcessedObject.label",
                        description = "PageSimulationResultObject.auth.simulationProcessedObject.description")
        }
)
public class PageSimulationResultObject extends PageAdmin implements SimulationPage {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSimulationResultObject.class);

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_DETAILS = "details";
    private static final String ID_CHANGES = "changes";

    private IModel<SimulationResultType> resultModel;

    private IModel<SimulationResultProcessedObjectType> objectModel;

    private IModel<List<DetailsTableItem>> detailsModel;

    private IModel<VisualizationDto> changesModel;

    public PageSimulationResultObject() {
        this(new PageParameters());
    }

    public PageSimulationResultObject(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResultObject.this);
            }
        };

        objectModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultProcessedObjectType load() {
                Task task = getPageTask();

                Long id = null;
                try {
                    id = Long.parseLong(getPageParameterContainerId());
                } catch (Exception ignored) {
                }

                if (id == null) {
                    throw new RestartResponseException(PageError404.class);
                }

                ObjectQuery query = getPrismContext().queryFor(SimulationResultProcessedObjectType.class)
                        .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                        .ownerId(resultModel.getObject().getOid())
                        .and()
                        .id(id)
                        .build();

                List<SimulationResultProcessedObjectType> result = WebModelServiceUtils.searchContainers(SimulationResultProcessedObjectType.class,
                        query, null, task.getResult(), PageSimulationResultObject.this);

                if (result.isEmpty()) {
                    throw new RestartResponseException(PageError404.class);
                }

                return result.get(0);
            }
        };

        detailsModel = new LoadableDetachableModel<>() {

            @Override
            protected List<DetailsTableItem> load() {
                List<DetailsTableItem> items = new ArrayList<>();

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.type"), () -> GuiSimulationsUtil.getProcessedObjectType(objectModel)));

                IModel<String> resourceCoordinatesModel = new LoadableDetachableModel<>() {

                    @Override
                    protected String load() {
                        SimulationResultProcessedObjectType object = objectModel.getObject();
                        ShadowDiscriminatorType discriminator = object.getResourceObjectCoordinates();
                        if (discriminator == null) {
                            return null;
                        }

                        ObjectReferenceType resourceRef = discriminator.getResourceRef();
                        if (resourceRef == null) {
                            return null;
                        }

                        PrismObject<ResourceType> resourceObject = WebModelServiceUtils.loadObject(resourceRef, PageSimulationResultObject.this);
                        if (resourceObject == null) {
                            return null;
                        }

                        ResourceType resource = resourceObject.asObjectable();
                        SchemaHandlingType handling = resource.getSchemaHandling();
                        if (handling == null) {
                            return null;
                        }

                        ResourceObjectTypeDefinitionType found = null;
                        for (ResourceObjectTypeDefinitionType objectType : handling.getObjectType()) {
                            if (Objects.equals(objectType.getKind(), discriminator.getKind()) && Objects.equals(objectType.getIntent(), discriminator.getIntent())) {
                                found = objectType;
                                break;
                            }
                        }

                        if (found == null) {
                            return null;
                        }

                        // todo probably use this to get display name?
                        // Resource.of(resourceObject)
                        //        .getCompleteSchemaRequired()
                        //        .findObjectDefinitionRequired(discriminator.getKind(), discriminator.getIntent())
                        //        .getDisplayName();
                        String displayName = found.getDisplayName();
                        if (displayName == null) {
                            displayName = getString("PageSimulationResultObject.unknownResourceObject");
                        }

                        return getString("PageSimulationResultObject.resourceCoordinatesValue", displayName, WebComponentUtil.getName(resource));
                    }
                };
                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.resourceCoordinates"), resourceCoordinatesModel) {

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(() -> StringUtils.isNotEmpty(resourceCoordinatesModel.getObject()));
                    }
                });

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.state"), null) {

                    @Override
                    public Component createValueComponent(String id) {
                        return GuiSimulationsUtil.createProcessedObjectStateLabel(id, objectModel);
                    }
                });

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.structuralArchetype"),
                        new LoadableDetachableModel<>() {
                            @Override
                            protected String load() {
                                SimulationResultProcessedObjectType object = objectModel.getObject();
                                if (object.getStructuralArchetypeRef() == null) {
                                    return null;
                                }

                                return WebModelServiceUtils.resolveReferenceName(object.getStructuralArchetypeRef(), PageSimulationResultObject.this);
                            }
                        }) {

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(() -> objectModel.getObject().getStructuralArchetypeRef() != null);
                    }
                });

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.marks"), null) {

                    @Override
                    public Component createValueComponent(String id) {
                        IModel<String> model = new LoadableDetachableModel<>() {

                            @Override
                            protected String load() {
                                SimulationResultProcessedObjectType object = objectModel.getObject();

                                Object[] names = object.getEventMarkRef().stream()
                                        .map(ref -> WebModelServiceUtils.resolveReferenceName(ref, PageSimulationResultObject.this))
                                        .filter(name -> name != null)
                                        .sorted()
                                        .toArray();

                                return StringUtils.joinWith("\n", names);
                            }
                        };

                        MultiLineLabel label = new MultiLineLabel(id, model);
                        label.setRenderBodyOnly(true);

                        return label;
                    }
                });

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.projectionCount"),
                        () -> "" + objectModel.getObject().getProjectionRecords()));

                // todo implement

                return items;
            }
        };

        changesModel = new LoadableDetachableModel<>() {

            @Override
            protected VisualizationDto load() {
                Visualization visualization;
                try {
                    ObjectDelta delta = DeltaConvertor.createObjectDelta(objectModel.getObject().getDelta());

                    Task task = getPageTask();
                    OperationResult result = task.getResult();

                    visualization = getModelInteractionService().visualizeDelta(delta, task, result);
                } catch (SchemaException | ExpressionEvaluationException e) {
                    LOGGER.debug("Couldn't convert and visualize delta", e);

                    throw new SystemException(e);
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Creating dto for deltas:\n{}", DebugUtil.debugDump(visualization));
                }

                final WrapperVisualization wrapper =
                        new WrapperVisualization(Arrays.asList(visualization), "PagePreviewChanges.primaryChangesOne", 1);

                return new VisualizationDto(wrapper);
            }
        };
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createTitleModel() {
                return () ->
                        WebComponentUtil.getOrigStringFromPoly(objectModel.getObject().getName())
                                + " (" + WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject()) + ")";
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResultObject.this.onBackPerformed(target);
            }
        };
        add(navigation);

        DetailsTablePanel details = new DetailsTablePanel(ID_DETAILS,
                () -> "fa-solid fa-circle-question",
                createStringResource("PageSimulationResultObject.details"),
                detailsModel);
        add(details);

        VisualizationPanel panel = new VisualizationPanel(ID_CHANGES, changesModel);
        add(panel);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    private void onBackPerformed(AjaxRequestTarget target) {
        redirectBack();
    }
}