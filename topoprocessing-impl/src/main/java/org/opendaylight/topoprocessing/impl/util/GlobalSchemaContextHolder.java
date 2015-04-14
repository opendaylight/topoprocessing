/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Stores global schema context
 * @author michal.polkorab
 */
public class GlobalSchemaContextHolder {

    private SchemaContext globalSchemaContext;
    private DataSchemaContextTree contextTree;

    /**
     * Default constructor
     * @param globalSchemaContext
     */
    public GlobalSchemaContextHolder(SchemaContext globalSchemaContext) {
        this.globalSchemaContext = globalSchemaContext;
        this.contextTree = DataSchemaContextTree.from(globalSchemaContext);
    }

    public void updateSchemaContext(SchemaContext globalSchemaContext) {
        this.globalSchemaContext = globalSchemaContext;
        this.contextTree = DataSchemaContextTree.from(globalSchemaContext);
    }

    /**
     * @return DataSchemaContextTree
     */
    public DataSchemaContextTree getContextTree() {
        return contextTree;
    }

    /**
     * @return global schema context
     */
    public SchemaContext getSchemaContext() {
        return globalSchemaContext;
    }

}