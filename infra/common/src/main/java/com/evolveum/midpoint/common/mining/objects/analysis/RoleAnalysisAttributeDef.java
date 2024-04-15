/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.analysis;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

public class RoleAnalysisAttributeDef implements Serializable {

    ItemPath path;
    boolean isContainer;
    String displayValue;
    ObjectQuery query;
    Class<? extends ObjectType> classType;
    IdentifierType identifierType;

    public RoleAnalysisAttributeDef(ItemPath path,
            boolean isContainer,
            Class<? extends ObjectType> classType) {
        this.path = path;
        this.isContainer = isContainer;
        this.classType = classType;
    }

    public RoleAnalysisAttributeDef(ItemPath path,
            boolean isContainer,
            Class<? extends ObjectType> classType,
            IdentifierType identifierType) {
        this.path = path;
        this.isContainer = isContainer;
        this.classType = classType;
        this.identifierType = identifierType;
    }

    public RoleAnalysisAttributeDef(ItemPath path,
            boolean isContainer,
            String displayValue,
            Class<? extends ObjectType> classType,
            IdentifierType identifierType) {
        this.path = path;
        this.isContainer = isContainer;
        this.displayValue = displayValue;
        this.classType = classType;
        this.identifierType = identifierType;
    }

    public ItemPath getPath() {
        return path;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

    public void setContainer(boolean container) {
        isContainer = container;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
        if (!isContainer) {
            Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);
            if (property != null) {
                Object object = property.getRealValue();
                return extractRealValue(object);
            }
        }
        return null;
    }

    public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
        Set<String> resolvedValues = new HashSet<>();
        Collection<Item<?, ?>> allItems = prismObject.getAllItems(itemPath);
        for (Item<?, ?> item : allItems) {
            Object realValue = item.getRealValue();
            if (realValue != null) {
                resolvedValues.add(realValue.toString());
            }
        }
        return resolvedValues;
    }

    public static String extractRealValue(Object object) {
        if (object != null) {
            if (object instanceof PolyString) {
                return ((PolyString) object).getOrig();
            } else if (object instanceof PrismPropertyValueImpl) {
                Object realValue = ((PrismPropertyValueImpl<?>) object).getRealValue();
                if (realValue != null) {
                    return realValue.toString();
                }
            } else {
                return object.toString();
            }
        }
        return null;
    }

    public ObjectQuery getQuery(String value) {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    public Class<? extends ObjectType> getClassType() {
        return classType;
    }

    public enum IdentifierType {
        OID,
        FINAL
    }

    public IdentifierType getIdentifierType() {
        return identifierType;
    }

}
