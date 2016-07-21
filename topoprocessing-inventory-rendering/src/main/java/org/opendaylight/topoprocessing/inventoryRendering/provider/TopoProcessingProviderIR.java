/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventoryRendering.provider;

import org.opendaylight.topoprocessing.inventoryRendering.adapter.IRModelAdapter;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.InventoryRenderingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopoProcessingProviderIR implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopoProcessingProviderIR.class);

    @Override
    public void close() throws Exception {
        LOGGER.info("TopoprocessingProviderIR close");
    }

    public void startup(TopoProcessingProvider topoProvider) {
        LOGGER.info("TopoprocessingProviderIR startup");
        topoProvider.registerModelAdapter(InventoryRenderingModel.class, new IRModelAdapter());
    }
}
