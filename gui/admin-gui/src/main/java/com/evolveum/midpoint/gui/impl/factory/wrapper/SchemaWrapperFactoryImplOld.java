/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.SchemaPropertyWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author katka
 */
@Component
public class SchemaWrapperFactoryImplOld
        extends PrismPropertyWrapperFactoryImpl<SchemaDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaWrapperFactoryImplOld.class);

    @Autowired protected SchemaService schemaService;

    private static final String DOT_CLASS = SchemaWrapperFactoryImplOld.class.getSimpleName() + ".";

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return QNameUtil.match(SchemaDefinitionType.COMPLEX_TYPE, def.getTypeName())
                && parent != null
                && QNameUtil.match(parent.getTypeName(), SchemaType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 100;
    }

    @Override
    protected PrismPropertyValue<SchemaDefinitionType> createNewValue(PrismProperty<SchemaDefinitionType> item) throws SchemaException {
        PrismPropertyValue<SchemaDefinitionType> newValue = getPrismContext().itemFactory().createPropertyValue();
        item.add(newValue);
        return newValue;
    }

    @Override
    protected PrismPropertyWrapper<SchemaDefinitionType> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismProperty<SchemaDefinitionType> item,
            ItemStatus status, WrapperContext wrapperContext) {
        PrismPropertyWrapper<SchemaDefinitionType> propertyWrapper = new PrismPropertyWrapperImpl<>(parent, item, status);
        return propertyWrapper;
    }


    @Override
    public SchemaPropertyWrapperImpl createValueWrapper(PrismPropertyWrapper<SchemaDefinitionType> parent, PrismPropertyValue<SchemaDefinitionType> value,
            ValueStatus status, WrapperContext context) {

        return new SchemaPropertyWrapperImpl(parent, value, status);
    }

    //TODO maybe special panel here?
    @Override
    public void registerWrapperPanel(PrismPropertyWrapper<SchemaDefinitionType> wrapper) {
        getRegistry().registerWrapperPanel(wrapper.getTypeName(), PrismPropertyPanel.class);
    }

}
