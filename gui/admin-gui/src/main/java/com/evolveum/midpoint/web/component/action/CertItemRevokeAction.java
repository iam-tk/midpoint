/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.action;

import com.evolveum.midpoint.web.application.GuiActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

@GuiActionType(
        identifier = "certItemRevoke",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertDecisions.menu.revoke", icon = "fa fa-times text-danger", order = 2),
        button = true)
public class CertItemRevokeAction extends AbstractCertItemAction {

    public CertItemRevokeAction() {
        super();
    }

    @Override
    protected AccessCertificationResponseType getResponse() {
        return AccessCertificationResponseType.REVOKE;
    }

}
