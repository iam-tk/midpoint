/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.panel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchModeType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;

public class TableCellFillOperation {

    public static void updateFrequencyUserBased(IModel<MiningRoleTypeChunk> rowModel, double minFrequency, double maxFrequency) {
        if (minFrequency > rowModel.getObject().getFrequency() && rowModel.getObject().getFrequency() < maxFrequency) {
            rowModel.getObject().setStatus(ClusterObjectUtils.Status.DISABLE);
        } else if (maxFrequency < rowModel.getObject().getFrequency()) {
            rowModel.getObject().setStatus(ClusterObjectUtils.Status.DISABLE);
        }
    }

    public static void updateFrequencyRoleBased(IModel<MiningUserTypeChunk> rowModel, double minFrequency, double maxFrequency) {
        if (minFrequency > rowModel.getObject().getFrequency() && rowModel.getObject().getFrequency() < maxFrequency) {
            rowModel.getObject().setStatus(ClusterObjectUtils.Status.DISABLE);
        } else if (maxFrequency < rowModel.getObject().getFrequency()) {
            rowModel.getObject().setStatus(ClusterObjectUtils.Status.DISABLE);
        }
    }

    public static void updateRoleBasedTableData(Item<ICellPopulator<MiningUserTypeChunk>> cellItem, String componentId,
            IModel<MiningUserTypeChunk> model, List<String> rowRoles, RoleAnalysisSearchModeType searchMode,
            ClusterObjectUtils.Status colStatus, List<String> colRoles, IntersectionObject intersection,
            MiningRoleTypeChunk roleChunk) {
        if (searchMode.equals(RoleAnalysisSearchModeType.INTERSECTION)) {
            intersectionMode(cellItem, componentId, model, rowRoles, colStatus, colRoles, intersection, roleChunk);
        } else {
            List<String> rowUsers = model.getObject().getUsers();
            jaccardMode(cellItem, componentId, model, rowUsers, rowRoles, colStatus, colRoles, intersection, roleChunk);
        }
    }

    private static void jaccardMode(Item<ICellPopulator<MiningUserTypeChunk>> cellItem, String componentId,
            IModel<MiningUserTypeChunk> model, List<String> rowUsers, List<String> rowRoles,
            ClusterObjectUtils.Status colStatus, List<String> colRoles, IntersectionObject intersection,
            MiningRoleTypeChunk roleChunk) {

        ClusterObjectUtils.Status status = model.getObject().getStatus();

        if (status.equals(ClusterObjectUtils.Status.DISABLE) || colStatus.equals(ClusterObjectUtils.Status.DISABLE)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "bg-danger", "");
        } else if (status.equals(ClusterObjectUtils.Status.NEUTRAL) || colStatus.equals(ClusterObjectUtils.Status.NEUTRAL)) {
            if (intersection != null && intersection.getElements() != null) {
                Set<String> roles = intersection.getPoints();
                Set<String> users = intersection.getElements();
                boolean containsAllRowUsers = users.containsAll(new HashSet<>(rowUsers));

                if (containsAllRowUsers && roles.containsAll(new HashSet<>(colRoles))) {
                    filledCell(cellItem, componentId, rowRoles, colRoles, "bg-success", "bg-warning");
                    model.getObject().setStatus(ClusterObjectUtils.Status.ADD);
                    roleChunk.setStatus(ClusterObjectUtils.Status.ADD);
                } else {
                    filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
                }
            } else {
                filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
            }
        } else if (status.equals(ClusterObjectUtils.Status.ADD) && colStatus.equals(ClusterObjectUtils.Status.ADD)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "bg-success", "bg-warning");
        } else if (status.equals(ClusterObjectUtils.Status.REMOVE) || colStatus.equals(ClusterObjectUtils.Status.REMOVE)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
        } else {
            filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
        }
    }

    private static void intersectionMode(Item<ICellPopulator<MiningUserTypeChunk>> cellItem, String componentId,
            IModel<MiningUserTypeChunk> model, List<String> rowRoles, ClusterObjectUtils.Status colStatus,
            List<String> colRoles, IntersectionObject intersection, MiningRoleTypeChunk roleChunk) {

        ClusterObjectUtils.Status status = model.getObject().getStatus();

        if (status.equals(ClusterObjectUtils.Status.DISABLE) || colStatus.equals(ClusterObjectUtils.Status.DISABLE)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "bg-danger", "");
        } else if (status.equals(ClusterObjectUtils.Status.NEUTRAL) || colStatus.equals(ClusterObjectUtils.Status.NEUTRAL)) {
            if (intersection != null && intersection.getPoints() != null) {
                Set<String> roles = intersection.getPoints();
                boolean found = new HashSet<>(roles).containsAll(colRoles) && new HashSet<>(rowRoles).containsAll(roles);

                if (found) {
                    model.getObject().setStatus(ClusterObjectUtils.Status.ADD);
                    roleChunk.setStatus(ClusterObjectUtils.Status.ADD);
                    filledCell(cellItem, componentId, rowRoles, colRoles, "bg-success", "table-dark");
                } else {
                    filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
                }
            } else {
                filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
            }
        } else if (status.equals(ClusterObjectUtils.Status.ADD) && colStatus.equals(ClusterObjectUtils.Status.ADD)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "bg-success", "bg-warning");
        } else if (status.equals(ClusterObjectUtils.Status.REMOVE) || colStatus.equals(ClusterObjectUtils.Status.REMOVE)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
        } else {
            filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
        }
    }

    public static void updateUserBasedTableData(Item<ICellPopulator<MiningRoleTypeChunk>> cellItem, String componentId,
            IModel<MiningRoleTypeChunk> model, List<String> rowUsers, RoleAnalysisSearchModeType searchMode,
            List<String> colUsers, IntersectionObject intersection,
            ClusterObjectUtils.Status colStatus, MiningUserTypeChunk userChunk) {
        if (searchMode.equals(RoleAnalysisSearchModeType.INTERSECTION)) {
            intersectionMode(cellItem, componentId, model, rowUsers, colUsers, intersection, colStatus, userChunk);
        } else {
            List<String> rowRoles = model.getObject().getRoles();
            jaccardMode(cellItem, componentId, model, rowUsers, rowRoles, colStatus, colUsers, intersection, userChunk);
        }
    }

    private static void intersectionMode(Item<ICellPopulator<MiningRoleTypeChunk>> cellItem, String componentId,
            IModel<MiningRoleTypeChunk> model, List<String> rowUsers, List<String> colUsers, IntersectionObject intersection,
            ClusterObjectUtils.Status colStatus, MiningUserTypeChunk userChunk) {

        ClusterObjectUtils.Status status = model.getObject().getStatus();

        if (status.equals(ClusterObjectUtils.Status.DISABLE) || colStatus.equals(ClusterObjectUtils.Status.DISABLE)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "bg-danger", "");
        } else if (status.equals(ClusterObjectUtils.Status.NEUTRAL) || colStatus.equals(ClusterObjectUtils.Status.NEUTRAL)) {
            if (intersection != null && intersection.getPoints() != null) {
                Set<String> users = intersection.getPoints();
                boolean found = users != null && new HashSet<>(users).containsAll(colUsers) && new HashSet<>(rowUsers).containsAll(users);

                if (found) {
                    model.getObject().setStatus(ClusterObjectUtils.Status.ADD);
                    userChunk.setStatus(ClusterObjectUtils.Status.ADD);
                    Tools.filledCell(cellItem, componentId, "bg-success");
                } else {
                    filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
                }
            } else {
                filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
            }
        } else if (status.equals(ClusterObjectUtils.Status.ADD) && colStatus.equals(ClusterObjectUtils.Status.ADD)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "bg-success", "bg-warning");
        } else if (status.equals(ClusterObjectUtils.Status.REMOVE) || colStatus.equals(ClusterObjectUtils.Status.REMOVE)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
        } else {
            filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
        }
    }

    private static void jaccardMode(Item<ICellPopulator<MiningRoleTypeChunk>> cellItem, String componentId,
            IModel<MiningRoleTypeChunk> model, List<String> rowUsers, List<String> rowRoles, ClusterObjectUtils.Status
            colStatus, List<String> colUsers, IntersectionObject intersection, MiningUserTypeChunk userChunk) {
        ClusterObjectUtils.Status status = model.getObject().getStatus();

        if (status.equals(ClusterObjectUtils.Status.DISABLE) || colStatus.equals(ClusterObjectUtils.Status.DISABLE)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "bg-danger", "");
        } else if (status.equals(ClusterObjectUtils.Status.NEUTRAL) || colStatus.equals(ClusterObjectUtils.Status.NEUTRAL)) {
            if (intersection != null && intersection.getElements() != null) {
                Set<String> roles = intersection.getElements();
                Set<String> users = intersection.getPoints();
                if (roles.containsAll(new HashSet<>(rowRoles))) {
                    if (users.containsAll(new HashSet<>(colUsers))) {
                        if (new HashSet<>(rowUsers).containsAll(new HashSet<>(colUsers))) {
                            Tools.filledCell(cellItem, componentId, "bg-success");
                        } else {
                            Tools.filledCell(cellItem, componentId, "bg-warning");
                        }
                        model.getObject().setStatus(ClusterObjectUtils.Status.ADD);
                        userChunk.setStatus(ClusterObjectUtils.Status.ADD);
                    } else {
                        filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
                    }
                } else {
                    filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
                }
            } else {
                filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
            }
        } else if (status.equals(ClusterObjectUtils.Status.ADD) && colStatus.equals(ClusterObjectUtils.Status.ADD)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "bg-success", "bg-warning");
        } else if (status.equals(ClusterObjectUtils.Status.REMOVE) || colStatus.equals(ClusterObjectUtils.Status.REMOVE)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
        } else {
            filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
        }

    }

    public static void filledCell(@NotNull Item<?> cellItem, String componentId, List<String> containsAll, List<String> contain, String colorTrue, String colorFalse) {
        String cellColor = new HashSet<>(containsAll).containsAll(new HashSet<>(contain)) ? colorTrue : colorFalse;
        cellItem.add(AttributeModifier.append("class", cellColor));
        cellItem.add(new EmptyPanel(componentId));
    }

}
