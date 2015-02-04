/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.provider;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.topoprocessing.impl.handler.CorrelationHandler;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;

/**
 * @author michal.polkorab
 *
 */
public class TopoProcessingProviderImpl implements TopoProcessingProvider {

    private CorrelationHandler correlationHandler;

    /**
     * @param schemaService
     */
    public TopoProcessingProviderImpl(SchemaService schemaService) {
        correlationHandler = new CorrelationHandler(schemaService.getGlobalContext());
    }

    @Override
    public void startup() {
        // start TopologyListener
        // register SchemaContextListener
    }

    @Override
    public void close() throws Exception {
        // shutdown TopologyListener
        // unregister SchemaContextListener
    }

}
