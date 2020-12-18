/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.testing;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.Database;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * This repository factory should be used for testing purposes only.
 * It behaves like {@link com.evolveum.midpoint.repo.sql.SqlRepositoryFactory}, but during
 * configuration initialization it checks system properties and overrides loaded configuration
 * ({@link com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration}).
 *
 * @author lazyman
 */
public class TestSqlRepositoryFactory extends SqlRepositoryFactory {

    private static final Trace LOGGER = TraceManager.getTrace(TestSqlRepositoryFactory.class);

    public static final String PROPERTY_CONFIG = "config";

    @Override
    public synchronized void init(Configuration configuration) throws RepositoryServiceFactoryException {
        String configFile = System.getProperty(PROPERTY_CONFIG);
        if (StringUtils.isNotEmpty(configFile)) {
            LOGGER.info("Overriding loaded configuration with values from '{}'", configFile);
            updateConfigurationFromFile(configuration, configFile);
        }

        updateConfigurationFromProperties(configuration, null);

        super.init(configuration);
    }

    private void updateConfigurationFromFile(Configuration configuration, String filePath)
            throws RepositoryServiceFactoryException {
        Properties properties = new Properties();
        try {
            File file = new File(filePath);
            LOGGER.debug("Config file absolute path '{}'.", file.getAbsolutePath());
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                throw new RepositoryServiceFactoryException("Config file '" + filePath + "' doesn't exist or can't be read.");
            }

            Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            properties.load(reader);
        } catch (RepositoryServiceFactoryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException(ex.getMessage(), ex);
        }

        //override loaded configuration based on properties file...
        updateConfigurationFromProperties(configuration, properties);
    }

    private void updateConfigurationFromProperties(Configuration configuration, Properties properties) {
        updateConfigurationStringProperty(configuration, properties, PROPERTY_DATABASE);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_EMBEDDED);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_DROP_IF_EXISTS);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_AS_SERVER);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_TCP_SSL);

        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_PORT);

        updateConfigurationStringProperty(configuration, properties, PROPERTY_BASE_DIR);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_FILE_NAME);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_DRIVER_CLASS_NAME);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_HIBERNATE_DIALECT);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_HIBERNATE_HBM2DDL);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_PASSWORD);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_URL);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_USERNAME);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_SKIP_EXPLICIT_SCHEMA_VALIDATION);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_MISSING_SCHEMA_ACTION);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_UPGRADEABLE_SCHEMA_ACTION);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_INCOMPATIBLE_SCHEMA_ACTION);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_SCHEMA_VERSION_IF_MISSING);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_SCHEMA_VERSION_OVERRIDE);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_SCHEMA_VARIANT);

        updateConfigurationStringProperty(configuration, properties, PROPERTY_TRANSACTION_ISOLATION);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_LOCK_FOR_UPDATE_VIA_SQL);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_READ_ONLY_TRANSACTIONS_STATEMENT);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_PERFORMANCE_STATISTICS_FILE);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_PERFORMANCE_STATISTICS_LEVEL);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_ITERATIVE_SEARCH_BY_PAGING);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_MAX_OBJECTS_FOR_IMPLICIT_FETCH_ALL_ITERATION_METHOD);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_USE_ZIP);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_FULL_OBJECT_FORMAT);
        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_MIN_POOL_SIZE);
        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_MAX_POOL_SIZE);

        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_TEXT_INFO_COLUMN_SIZE);

        // Dirty hack, in order to make DataSourceTest happy: if none of database, driver, dialect, embedded is
        // present but data source is, let us assume we use H2.
        //
        // The reason is that when using datasource (and without the dialect set) we do not have the usual information
        // we could use to derive the database. We do not want to default to H2, as it could cause problems in
        // production. So we switch to H2 in such cases only in the test mode - i.e. here.

        if (!configuration.containsKey(PROPERTY_DATABASE)
                && !configuration.containsKey(PROPERTY_DRIVER_CLASS_NAME)
                && !configuration.containsKey(PROPERTY_HIBERNATE_DIALECT)
                && !configuration.containsKey(PROPERTY_EMBEDDED)
                && configuration.containsKey(PROPERTY_DATASOURCE)) {
            configuration.setProperty(PROPERTY_DATABASE, Database.H2.name());
        }
    }

    private void updateConfigurationIntegerProperty(
            Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null || !value.matches("[1-9][0-9]*")) {
            return;
        }
        int val = Integer.parseInt(value);
        LOGGER.info("Overriding loaded configuration with value read from system properties: {}={}", propertyName, val);
        configuration.setProperty(propertyName, val);
    }

    private void updateConfigurationBooleanProperty(
            Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null) {
            return;
        }
        boolean val = Boolean.parseBoolean(value);
        LOGGER.info("Overriding loaded configuration with value read from system properties: {}={}", propertyName, val);
        configuration.setProperty(propertyName, val);
    }

    private void updateConfigurationStringProperty(
            Configuration configuration, Properties properties, String propertyName) {
        updateConfigurationStringProperty(configuration, properties, propertyName, null);
    }

    private void updateConfigurationStringProperty(
            Configuration configuration, Properties properties, String propertyName,
            @SuppressWarnings("SameParameterValue") String defaultValue) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null) {
            value = defaultValue;
        }

        if (value == null) {
            return;
        }
        LOGGER.info("Overriding loaded configuration with value read from system properties: {}={}", propertyName, value);
        configuration.setProperty(propertyName, value);
    }
}
