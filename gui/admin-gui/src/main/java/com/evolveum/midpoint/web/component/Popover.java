/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Popover extends Border {

    private static final String ID_POPOVER = "popover";
    private static final String ID_TITLE = "title";

    private Component reference;

    private IModel<String> title;

    private IModel<Boolean> visible = new Model<>(false);

    public Popover(String id) {
        this(id, null);
    }

    public Popover(String id, IModel<String> title) {
        super(id);

        this.title = title != null ? title : new Model<>();

        initLayout();
    }

    public void setReference(Component reference) {
        this.reference = reference;
    }

    private void initLayout() {
        setOutputMarkupId(true);
        add(AttributeAppender.prepend("class", "popover bs-popover-bottom"));
        add(AttributeAppender.append("style", "display: none;"));

        Label title = new Label(ID_TITLE, this.title);
        title.add(new VisibleBehaviour(() -> Popover.this.title.getObject() != null));
        addToBorder(title);
    }

    public boolean isPopoverVisible() {
        return visible.getObject();
    }

    public void toggle(AjaxRequestTarget target) {
        if (isPopoverVisible()) {
            hide(target);
        } else {
            show(target);
        }
    }

    public void show(AjaxRequestTarget target) {
        if (isPopoverVisible()) {
            return;
        }

        visible.setObject(true);

        callJavascript(target, true);
    }

    public void hide(AjaxRequestTarget target) {
        if (!isPopoverVisible()) {
            return;
        }

        visible.setObject(false);

        callJavascript(target, false);
    }

    private void callJavascript(AjaxRequestTarget target, boolean show) {
        target.appendJavaScript("$(function() { MidPointTheme.showPopover('#" + reference.getMarkupId() + "', '#" + getMarkupId() + "', " + show + "); });");
    }
}
