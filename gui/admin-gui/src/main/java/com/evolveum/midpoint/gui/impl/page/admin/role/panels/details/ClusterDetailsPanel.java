/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.panels.details;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.jaccSortMiningSet;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.MiningObjectUtils.getMiningObject;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.MiningObjectUtils.getRoleObject;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.SimilarGroupDetailsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class ClusterDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";

    private static final String ID_DATATABLE = "datatable_extra";

    List<String> similarGroupOid;
    String targetValue;

    public ClusterDetailsPanel(String id, IModel<String> messageModel,
            List<String> similarGroupOid, String targetValue) {
        super(id, messageModel);
        this.similarGroupOid = similarGroupOid;
        this.targetValue = targetValue;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        String string = DOT_CLASS + "getMiningTypeObject";
        OperationResult result = new OperationResult(string);
        List<PrismObject<MiningType>> miningTypeList = new ArrayList<>();
        Set<String> rolesOid = new HashSet<>();
        for (String groupOid : similarGroupOid) {
            PrismObject<MiningType> miningObject = getMiningObject(getPageBase(), groupOid, result);
            miningTypeList.add(miningObject);
            rolesOid.addAll(miningObject.asObjectable().getRoles());
        }

        if (targetValue != null) {
            PrismObject<MiningType> miningObject = getMiningObject(getPageBase(), targetValue, result);
            miningTypeList.add(miningObject);
            rolesOid.addAll(miningObject.asObjectable().getRoles());
        }

        List<PrismObject<RoleType>> rolePrismObjectList = new ArrayList<>();
        for (String oid : rolesOid) {
            PrismObject<RoleType> roleObject = getRoleObject(getPageBase(), oid, result);
            if (roleObject != null) {
                rolePrismObjectList.add(roleObject);
            }
        }

        List<PrismObject<MiningType>> jaccSortMiningSet = jaccSortMiningSet(miningTypeList);

        Map<PrismObject<RoleType>, Long> roleCountMap = rolePrismObjectList.stream()
                .collect(Collectors.toMap(Function.identity(),
                        role -> jaccSortMiningSet.stream()
                                .filter(miningType -> miningType.asObjectable().getRoles().contains(role.getOid()))
                                .count()));

        List<PrismObject<RoleType>> sortedRolePrismObjectList = roleCountMap.entrySet().stream()
                .sorted(Map.Entry.<PrismObject<RoleType>, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        Component table = new SimilarGroupDetailsPanel(ID_DATATABLE, jaccSortMiningSet, sortedRolePrismObjectList,
                targetValue, false).add(new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                target.appendJavaScript(getScaleScript());

            }

            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));

            }
        });

        table.add(new AjaxEventBehavior("change") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                target.appendJavaScript(getScaleScript());
            }
        });
        add(table);

        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClose(target);
            }
        };
        add(cancelButton);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 1700;
    }

    @Override
    public int getHeight() {
        return 1100;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("Details.panel");
    }

    private String getScaleScript() {
        return "let div = document.querySelector('#myTable');" +
                "let table = div.querySelector('table');" +
                "let scale = 1;" +
                "if (div && table) {" +
                "  div.onwheel = function(e) {" +
                "    e.preventDefault();" +
                "    let rectBefore = table.getBoundingClientRect();" +
                "    let x = (e.clientX - rectBefore.left) / rectBefore.width * 100;" +
                "    let y = (e.clientY - rectBefore.top) / rectBefore.height * 100;" +
                "    table.style.transformOrigin = 'left top';" +
                "    if (e.deltaY < 0) {" +
                "      console.log('Zooming in');" +
                "      scale += 0.03;" +
                "      let prevScale = scale - 0.1;" +
                "      let scaleFactor = scale / prevScale;" +
                "      let deltaX = (x / 100) * rectBefore.width * (scaleFactor - 1);" +
                "      let deltaY = (y / 100) * rectBefore.height * (scaleFactor - 1);" +
                "      table.style.transformOrigin = x + '%' + ' ' + y + '%';" +
                "      table.style.transition = 'transform 0.3s';" + // Add transition property
                "      table.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = table.getBoundingClientRect();" +
                "      div.scrollLeft += (rectAfter.left - rectBefore.left) + deltaX - (e.clientX - rectBefore.left) * (scaleFactor - 1);" +
                "      div.scrollTop += (rectAfter.top - rectBefore.top) + deltaY - (e.clientY - rectBefore.top) * (scaleFactor - 1);" +
                "    } else if (e.deltaY > 0) {" +
                "      console.log('Zooming out');" +
                "      scale -= 0.03;" +
                "      scale = Math.max(0.1, scale);" +
                "      table.style.transition = 'transform 0.3s';" + // Add transition property
                "      table.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = table.getBoundingClientRect();" +
                "      div.scrollLeft += (rectAfter.left - rectBefore.left);" +
                "      div.scrollTop += (rectAfter.top - rectBefore.top);" +
                "    }" +
                "  };" +
                "} else {" +
                "  console.error('Div or table not found');" +
                "}";
    }

}
