/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Stores global schema context
 * @author michal.polkorab
 */
public class GlobalSchemaContextHolder {

    private static SchemaContext globalSchemaContext;

    /**
     * @return global schema context
     */
    public static SchemaContext getSchemaContext() {
        return globalSchemaContext;
    }

    /**
     * Sets global schema context
     * @param context global schema context
     */
    public static void setSchemaContext(SchemaContext context) {
        globalSchemaContext = context;
    }
}