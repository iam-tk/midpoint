/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.launcher;

import org.springframework.boot.loader.tools.Layouts;

public class MidPointWarLayout extends Layouts.War implements MidPointLayoutCommon {

    @Override
    public String getLauncherClassName() {
        return MidPointWarLauncher.class.getName();
    }
}