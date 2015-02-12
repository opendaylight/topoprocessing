/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.topoprocessing.impl.listener.GlobalSchemaContextListener;
import org.opendaylight.topoprocessing.impl.listener.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

/**
 * @author michal.polkorab
 *
 */
public class TopoProcessingProviderImpl implements TopoProcessingProvider {

    private SchemaService schemaService;
    private ListenerRegistration<SchemaContextListener> schemaContextListenerRegistration;
    private DataBroker dataBroker;
    private ListenerRegistration<DataChangeListener> topologyRequestListenerRegistration;

    /**
     * @param schemaService
     * @param dataBroker 
     */
    public TopoProcessingProviderImpl(SchemaService schemaService, DataBroker dataBroker) {
        this.schemaService = schemaService;
        this.dataBroker = dataBroker;
        GlobalSchemaContextHolder.setSchemaContext(schemaService.getGlobalContext());
        startup();
    }

    @Override
    public void startup() {
        registerTopologyRequestListener();
        schemaContextListenerRegistration =
                schemaService.registerSchemaContextListener(new GlobalSchemaContextListener());
    }

    @Override
    public void close() throws Exception {
        topologyRequestListenerRegistration.close();
        schemaContextListenerRegistration.close();
    }

    private void registerTopologyRequestListener() {
        InstanceIdentifier<Topology> identifier =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class);
        topologyRequestListenerRegistration =
                dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        identifier, new TopologyRequestListener(), DataChangeScope.BASE);
    }
}
