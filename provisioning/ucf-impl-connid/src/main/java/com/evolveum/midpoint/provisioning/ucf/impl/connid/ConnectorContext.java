/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

/**
 * A restricted access to basic data about the {@link ConnectorInstanceConnIdImpl}.
 * Created to avoid circular dependencies from called components to the connector instance,
 * yet allowing access to important data structures, like the resource schema.
 */
interface ConnectorContext {

    Boolean getConfiguredLegacySchema();

    boolean isLegacySchema();

    String getHumanReadableName();
}
