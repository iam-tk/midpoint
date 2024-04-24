/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.gui.impl.component.tile.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.CertCampaignsStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.io.Serial;

import static com.evolveum.midpoint.gui.api.page.PageAdminLTE.createStringResourceStatic;

public class CampaignsPanel extends BasePanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CAMPAIGNS_PANEL = "campaignsPanel";
    private LoadableDetachableModel<Search> searchModel;
    private SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider;

    public CampaignsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        searchModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Search load() {
                CertCampaignsStorage storage = getCampaignsStorage();
                if (storage == null || storage.getSearch() == null) {
                    return createSearch();
                }
                return storage.getSearch();
            }
        };
    }

    private void initLayout() {
        setOutputMarkupId(true);

        provider = createProvider();
        MultiSelectTileTablePanel<AccessCertificationCampaignType,
                AccessCertificationCampaignType> tilesTable =
                new MultiSelectTileTablePanel<>(ID_CAMPAIGNS_PANEL, createViewToggleModel(), UserProfileStorage.TableId.PAGE_CAMPAIGNS) {

                    @Serial private static final long serialVersionUID = 1L;
                    @Override
                    protected List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> createColumns() {
                        return CampaignsPanel.this.initColumns();
                    }

                    @Override
                    protected Component createHeader(String id) {
                        Component header = super.createHeader(id);
                        if (header instanceof SearchPanel) {
                            // mt-2 added because search panel now uses *-sm classes and it doesn't match rest of the layout
                            header.add(AttributeAppender.append("class", "mt-2"));
                        }

                        return header;
                    }

                    @Override
                    protected String getTilesContainerAdditionalClass() {
                        return null;
                    }

                    @Override
                    protected Component createTile(String id,
                            IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
                        return new CampaignTilePanel(id, model);
                    }

                    @Override
                    protected boolean isSelectedItemsPanelVisible() {
                        return false;
                    }

                    @Override
                    protected SelectableBeanObjectDataProvider<AccessCertificationCampaignType> createProvider() {
                        return provider;
                    }

                    @Override
                    protected String getTileCssClasses() {
                        return "col-12 col-md-6 col-lg-4 col-xxl-6i px-2";
                    }

                    @Override
                    protected IModel<Search> createSearchModel() {
                        return searchModel;
                    }

                    @Override
                    protected IModel<String> getItemLabelModel(AccessCertificationCampaignType entry) {
                        return Model.of(entry.getName().getOrig());
                    }

                    @Override
                    protected void deselectItem(AccessCertificationCampaignType entry) {

                    }

                    @Override
                    protected IModel<List<AccessCertificationCampaignType>> getSelectedItemsModel() {
                        return () -> new ArrayList<>(getProvider().getSelected());
                    }

                    @Override
                    protected boolean isTogglePanelVisible() {
                        return true;
                    }

                };
        add(tilesTable);
    }

    private SelectableBeanObjectDataProvider<AccessCertificationCampaignType> createProvider() {
        SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), () -> searchModel.getObject(), null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getCampaignsStorage();
            }

//            @Override
//            protected ObjectQuery getCustomizeContentQuery() {
//                return getCustomQuery();
//            }

            @Override
            public void detach() {
                preprocessSelectedDataInternal();
                super.detach();
            }

        };
        return provider;
    }

    private IModel<ViewToggle> createViewToggleModel() {
        return new LoadableModel<>(false) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ViewToggle load() {
                CertCampaignsStorage storage = getCampaignsStorage();
                return storage != null ? storage.getViewToggle() : ViewToggle.TILE;
            }
        };
    }

    private CertCampaignsStorage getCampaignsStorage() {
        return getPageBase().getSessionStorage().getCertCampaigns();
    }

    private List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> initColumns() {
        List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<AccessCertificationCampaignType>, String> column;

        column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = new AjaxLinkColumn<>(createStringResource("PageCertCampaigns.table.name"),
                SelectableBeanImpl.F_VALUE + "." + AccessCertificationCampaignType.F_NAME.getLocalPart()) {
            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                CampaignProcessingHelper.campaignDetailsPerformed(rowModel.getObject().getValue().getOid(), getPageBase());
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.description"),
                SelectableBeanImpl.F_VALUE + "." + AccessCertificationCampaignType.F_DESCRIPTION.getLocalPart());
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.iteration"),
                SelectableBeanImpl.F_VALUE + "." + AccessCertificationCampaignType.F_ITERATION.getLocalPart());
        columns.add(column);

        column = new EnumPropertyColumn<>(createStringResource("PageCertCampaigns.table.state"),
                SelectableBeanImpl.F_VALUE + "." + AccessCertificationCampaignType.F_STATE.getLocalPart()) {
            @Override
            protected String translate(Enum<?> en) {
                return createStringResourceStatic(getPage(), en).getString();
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.stage"),
                SelectableBeanImpl.F_VALUE + "." + AccessCertificationCampaignType.F_STAGE_NUMBER.getLocalPart()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccessCertificationCampaignType>>> item,
                    String componentId, IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                super.populateItem(item, componentId, rowModel);
                add(new VisibleBehaviour(() -> isStageVisible(rowModel.getObject())));
            }

            private boolean isStageVisible(SelectableBean<AccessCertificationCampaignType> rowModel) {
                AccessCertificationCampaignType campaign = rowModel.getValue();
                return campaign.getState() == AccessCertificationCampaignStateType.IN_REVIEW_STAGE ||
                        campaign.getState() == AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;
            }

        };
        columns.add(column);

        column = new AbstractColumn<>(createStringResource("PageCertCampaigns.table.escalationLevel")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccessCertificationCampaignType>>> item,
                    String componentId, IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                AccessCertificationCampaignType campaign = rowModel.getObject().getValue();
                int escalationLevelNumber = CertCampaignTypeUtil.getCurrentStageEscalationLevelNumberSafe(campaign);
                item.add(new Label(componentId, escalationLevelNumber));

                add(new VisibleBehaviour(() -> isEscalationLevelNumber(escalationLevelNumber)));
            }

            private boolean isEscalationLevelNumber(int escalationLevelNumber) {
                return escalationLevelNumber != 0;
            }
        };
        columns.add(column);

        column = new AbstractColumn<>(createStringResource("PageCertCampaigns.table.stages")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccessCertificationCampaignType>>> item,
                    String componentId, IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                AccessCertificationCampaignType campaign = rowModel.getObject().getValue();
                item.add(new Label(componentId, CertCampaignTypeUtil.getNumberOfStages(campaign)));
            }
        };
        columns.add(column);

        column = new AbstractColumn<>(createStringResource("PageCertCampaigns.table.deadline")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccessCertificationCampaignType>>> item,
                    String componentId, IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                AccessCertificationCampaignType campaign = rowModel.getObject().getValue();
                item.add(new Label(componentId, CampaignProcessingHelper.computeDeadlineAsString(campaign, getPageBase())));
            }
        };
        columns.add(column);

        column = new SingleButtonColumn<>(new Model<>(), null) {

            @Override
            public boolean isButtonEnabled(IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                //todo fix
//                final AccessCertificationCampaignType campaign = model.getObject().getValue();
//                String button = determineAction(campaign);
//                return button != null;
                return true;
            }

            @Override
            public boolean isButtonVisible(IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                final AccessCertificationCampaignType campaign = model.getObject().getValue();

                return campaign.getState() != AccessCertificationCampaignStateType.IN_REMEDIATION
                        && campaign.getState() != AccessCertificationCampaignStateType.CLOSED;
            }

            @Override
            public String getCaption() {
                //todo fix
//                AccessCertificationCampaignType campaign = getRowModel().getObject().getValue();
//                String button = determineAction(campaign);
//                if (button != null) {
//                    return CampaignsPanel.this.createStringResource(button).getString();
//                } else {
                    return "";
//                }
            }

            @Override
            public String getButtonCssColorClass() {
                return DoubleButtonColumn.ButtonColorClass.PRIMARY.toString();
            }

            @Override
            public String getButtonCssSizeClass() {
                return DoubleButtonColumn.ButtonSizeClass.SMALL.toString();
            }

            @Override
            public void clickPerformed(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                AccessCertificationCampaignType campaign = model.getObject().getValue();
                CampaignProcessingHelper.campaignActionPerformed(campaign, getPageBase(), target);
            }
        };
        columns.add(column);

        List<InlineMenuItem> inlineMenuItems = createInlineMenu();
        inlineMenuItems.addAll(createInlineMenuForItem());

        InlineMenuButtonColumn<SelectableBean<AccessCertificationCampaignType>> actionsColumn =
                new InlineMenuButtonColumn<>(inlineMenuItems, getPageBase());
        columns.add(actionsColumn);

        return columns;
    }

    private List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.startSelected")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.startSelectedCampaignsPerformed(target, getSelectedCampaigns(), getPageBase());
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.closeSelected")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.closeSelectedCampaignsConfirmation(target, getSelectedCampaigns(), getPageBase());
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.reiterateSelected")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.reiterateSelectedCampaignsConfirmation(target, getSelectedCampaigns(),
                                getPageBase());
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.deleteSelected")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.deleteSelectedCampaignsConfirmation(target, getSelectedCampaigns(), getPageBase());
                    }
                };
            }

        });
        return items;
    }

    private List<InlineMenuItem> createInlineMenuForItem() {

        List<InlineMenuItem> menuItems = new ArrayList<>();
        InlineMenuItem item = new InlineMenuItem(createStringResource("PageCertCampaigns.menu.close")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<AccessCertificationCampaignType>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.closeCampaignConfirmation(target, getRowModel().getObject(), getPageBase());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

        };
//        item.setVisibilityChecker((rowModel, header) -> isNotClosed(rowModel));
        menuItems.add(item);

        item = new InlineMenuItem(createStringResource("PageCertCampaigns.menu.reiterate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<AccessCertificationCampaignType>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.reiterateCampaignConfirmation(target, getRowModel().getObject(), getPageBase());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
//        item.setVisibilityChecker((rowModel, header) -> isReiterable(rowModel));
        menuItems.add(item);

        menuItems.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<AccessCertificationCampaignType>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.deleteCampaignConfirmation(target, getRowModel().getObject(), getPageBase());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

        });
        return menuItems;
    }

    private Search createSearch() {
        SearchBuilder searchBuilder = new SearchBuilder(AccessCertificationCampaignType.class)
                .modelServiceLocator(getPageBase());

        return searchBuilder.build();
    }

    private List<AccessCertificationCampaignType> getSelectedCampaigns() {
        return new ArrayList<>(provider.getSelected());
    }
}
