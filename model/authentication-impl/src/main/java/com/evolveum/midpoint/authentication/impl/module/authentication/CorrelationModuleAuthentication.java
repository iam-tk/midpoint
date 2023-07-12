/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;

import java.util.List;

public class CorrelationModuleAuthentication extends CredentialModuleAuthenticationImpl {

    private List<ModuleItemConfigurationType> moduleConfiguration;

    public CorrelationModuleAuthentication(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.CORRELATION, sequenceModule);
        setSufficient(false);
    }

    public ModuleAuthenticationImpl clone() {
        CorrelationModuleAuthentication module = new CorrelationModuleAuthentication(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        module.setModuleConfiguration(this.getModuleConfiguration());
        super.clone(module);
        return module;
    }

    public void setModuleConfiguration(List<ModuleItemConfigurationType> moduleConfiguration) {
        this.moduleConfiguration = moduleConfiguration;
    }

    public List<ModuleItemConfigurationType> getModuleConfiguration() {
        return moduleConfiguration;
    }
}
