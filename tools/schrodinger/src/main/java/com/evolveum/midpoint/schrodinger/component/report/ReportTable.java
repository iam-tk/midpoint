/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.report;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.report.ListReportsPage;
import com.evolveum.midpoint.schrodinger.page.report.ReportPage;

/**
 * Created by honchar
 */
public class ReportTable extends AssignmentHolderObjectListTable<ListReportsPage, ReportPage> {

    public ReportTable(ListReportsPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    protected TableHeaderDropDownMenu<ReportTable> clickHeaderActionDropDown() {
       return null;
    }

    @Override
    public ReportPage getObjectDetailsPage() {
        return new ReportPage();
    }

}

