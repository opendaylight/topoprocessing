/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.nt.provider;

import org.opendaylight.topoprocessing.nt.adapter.NTModelAdapter;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopoProcessingProviderNT implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopoProcessingProviderNT.class);

    @Override
    public void close() throws Exception {
        LOGGER.info("TopoprocessingProviderNT close");
    }

    public void startup(TopoProcessingProvider topoProvider) {
        LOGGER.info("TopoprocessingProviderNT startup");
        topoProvider.registerModelAdapter(NetworkTopologyModel.class, new NTModelAdapter());
    }
}
