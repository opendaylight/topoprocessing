/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens on {@link SchemaContext} update and updates
 * existing / stored {@link SchemaContext}
 * 
 * @author michal.polkorab
 */
public class GlobalSchemaContextListener implements SchemaContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSchemaContextListener.class);
    private GlobalSchemaContextHolder schemaHolder;

    /**
     * @param schemaHolder which holds global schema context and updates it
     * based on this ({@link GlobalSchemaContextListener})
     */
    public GlobalSchemaContextListener(GlobalSchemaContextHolder schemaHolder) {
        this.schemaHolder = schemaHolder;
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        LOGGER.debug("SchemaContext updated");
        schemaHolder.updateSchemaContext(context);
    }

}