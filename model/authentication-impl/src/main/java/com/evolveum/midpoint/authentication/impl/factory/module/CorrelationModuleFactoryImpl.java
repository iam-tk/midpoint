/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.impl.module.authentication.CorrelationModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configurer.CorrelationModuleWebSecurityConfigurer;

import com.evolveum.midpoint.authentication.impl.provider.CorrelationProvider;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class CorrelationModuleFactoryImpl extends AbstractCredentialModuleFactory
        <LoginFormModuleWebSecurityConfiguration, CorrelationModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration>> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof FocusIdentificationAuthenticationModuleType;
    }

    @Override
    protected LoginFormModuleWebSecurityConfiguration createConfiguration(AbstractAuthenticationModuleType moduleType, String prefixOfSequence, AuthenticationChannel authenticationChannel) {
        LoginFormModuleWebSecurityConfiguration configuration = LoginFormModuleWebSecurityConfiguration.build(moduleType,prefixOfSequence);
        configuration.setSequenceSuffix(prefixOfSequence);
        return configuration;
    }

    @Override
    protected CorrelationModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration> createModule(
            LoginFormModuleWebSecurityConfiguration configuration) {
        return  getObjectObjectPostProcessor().postProcess(new CorrelationModuleWebSecurityConfigurer<>(configuration));
    }

    @Override
    protected AuthenticationProvider createProvider(CredentialPolicyType usedPolicy) {
        return new CorrelationProvider();
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return null;
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(AbstractAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule) {
        CorrelationModuleAuthentication moduleAuthentication = new CorrelationModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(((AbstractCredentialAuthenticationModuleType)moduleType).getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
//        if (moduleType instanceof FocusIdentificationAuthenticationModuleType) {
//            moduleAuthentication.setModuleConfiguration(((FocusIdentificationAuthenticationModuleType) moduleType).getItem());
//        }
        return moduleAuthentication;
    }

}
