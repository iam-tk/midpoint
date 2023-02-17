/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AssignmentPathPanel extends BasePanel<List<AssignmentPathMetadataType>> {
    private static final String ID_PATHS = "paths";
    private static final String ID_PATH = "path";

    public AssignmentPathPanel(String id, IModel<List<AssignmentPathMetadataType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ListView<AssignmentPathMetadataType> assignmentPaths = new ListView<>(ID_PATHS, getModel()) {
            @Override
            protected void populateItem(ListItem<AssignmentPathMetadataType> listItem) {
                listItem.add(new AssignmentPathSegmentPanel(ID_PATH, loadSegmentsModel(listItem.getModel())));
            }
        };
        add(assignmentPaths);
    }

    private IModel<List<AssignmentPathSegmentMetadataType>> loadSegmentsModel(IModel<AssignmentPathMetadataType> assignmentModel) {
        return () -> {
            AssignmentPathMetadataType assignmentPathType = assignmentModel.getObject();
           List<AssignmentPathSegmentMetadataType> segments = assignmentPathType.getSegment();
           if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
               return new ArrayList<>();
           }
           return segments;
        };
    }
}