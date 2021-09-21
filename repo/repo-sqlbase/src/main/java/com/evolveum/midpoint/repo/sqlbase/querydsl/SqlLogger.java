/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.util.List;
import java.util.stream.Collectors;

import com.querydsl.sql.SQLBaseListener;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLListenerContext;
import com.querydsl.sql.types.Null;
import org.apache.commons.lang3.ArrayUtils;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Logger for Querydsl executed queries, set to DEBUG to log queries or to TRACE
 * to log parameter values as well (this causes additional formatting overhead).
 */
public class SqlLogger extends SQLBaseListener {

    private static final Trace LOGGER = TraceManager.getTrace(SqlLogger.class);

    /**
     * End is the right phase common to both selects and insert/updates.
     * It's called after exceptions too.
     */
    @Override
    public void end(SQLListenerContext context) {
        if (LOGGER.isDebugEnabled()) {
            try {
                logContext(context);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, e);
            }
        }
    }

    private void logContext(SQLListenerContext context) {
        SQLBindings sqlBindings = context.getSQLBindings();

        // nulls are unlikely in the render phase, but just to be sure (JVM will optimize it ;-))
        if (sqlBindings == null || sqlBindings.getSQL() == null) {
            return;
        }

        // replacing new-lines for spaces, we don't want multiline log
        LOGGER.debug(sqlBindings.getSQL().replace('\n', ' '));

        if (LOGGER.isTraceEnabled()) {
            List<Object> paramValues = sqlBindings.getNullFriendlyBindings();
            if (paramValues != null && !paramValues.isEmpty()) {
                LOGGER.trace(paramValues.stream()
                        .map(this::valueToString)
                        .collect(Collectors.joining(", ", "(", ")")));
            }
            // context.getMetadata().getWhere(); this is also interesting alternative
            // limit is not part of where, it's in metadata.modifiers
        }
    }

    public static final int BYTE_ARRAY_PREVIEW_LEN = 20;

    protected String valueToString(Object o) {
        if (o == null) {
            return null; // unlikely
        } else if (o instanceof String) {
            return '"' + (String) o + '"';
        } else if (o instanceof Null) {
            return "NULL";
        } else if (o.getClass() == byte[].class) {
            return MiscUtil.binaryToHexPreview((byte[]) o, BYTE_ARRAY_PREVIEW_LEN);
        } else if (o.getClass().isArray()) {
            return ArrayUtils.toString(o);
        } else {
            return o.toString();
        }
    }
}
