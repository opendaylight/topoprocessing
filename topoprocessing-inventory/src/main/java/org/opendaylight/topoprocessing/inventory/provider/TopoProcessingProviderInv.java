/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventory.provider;

import org.opendaylight.topoprocessing.inventory.adapter.InvModelAdapter;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopoProcessingProviderInv implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopoProcessingProviderInv.class);

    @Override
    public void close() throws Exception {
        LOGGER.info("TopoprocessingProviderInv close");
    }

    public void startup(TopoProcessingProvider topoProvider) {
        LOGGER.info("TopoprocessingProviderInv startup");
        topoProvider.registerModelAdapter(OpendaylightInventoryModel.class, new InvModelAdapter());
    }
}
