/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.provider;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.topoprocessing.impl.listener.GlobalSchemaContextListener;
import org.opendaylight.topoprocessing.impl.listener.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * @author michal.polkorab
 *
 */
public class TopoProcessingProviderImpl implements TopoProcessingProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopoProcessingProviderImpl.class);

    private DOMDataBroker dataBroker;
    private ListenerRegistration<DOMDataChangeListener> topologyRequestListenerRegistration;
    private SchemaService schemaService;
    private ListenerRegistration<SchemaContextListener> schemaContextListenerRegistration;

    /**
     * @param schemaService 
     * @param dataBroker
     */
    public TopoProcessingProviderImpl(SchemaService schemaService, DOMDataBroker dataBroker) {
        LOGGER.error("TOPOPROCESSINGPROVIDER");
        LOGGER.error("Creating TopoProcessingProvider");
        Preconditions.checkNotNull(schemaService, "SchemaService can't be null");
        Preconditions.checkNotNull(schemaService, "DOMDataBroker can't be null");
        this.schemaService = schemaService;
        this.dataBroker = dataBroker;
        GlobalSchemaContextHolder.setSchemaContext(schemaService.getGlobalContext());
        System.exit(0);
        startup();
    }

    @Override
    public void startup() {
        LOGGER.error("TopoProcessingProvider - startup()");
        schemaContextListenerRegistration =
                schemaService.registerSchemaContextListener(new GlobalSchemaContextListener());
        registerTopologyRequestListener();
    }

    @Override
    public void close() throws Exception {
        LOGGER.error("TopoProcessingProvider - close()");
        schemaContextListenerRegistration.close();
        topologyRequestListenerRegistration.close();
        LOGGER.error("TopoProcessingProvider - successfully closed");
    }

    private void registerTopologyRequestListener() {
        LOGGER.error("Registering Topology Request Listener");
        YangInstanceIdentifier identifier =
                YangInstanceIdentifier.of(NetworkTopology.QNAME).node(Topology.QNAME);

        topologyRequestListenerRegistration =
                dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        identifier, new TopologyRequestListener(dataBroker), DataChangeScope.BASE);
        LOGGER.error("Topology Request Listener has been successfully registered");
    }
}
