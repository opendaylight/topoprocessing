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
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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
    private GlobalSchemaContextHolder schemaHolder;
    private TopologyRequestListener topologyRequestListener;

    /**
     * @param schemaService
     * @param dataBroker
     * @param nodeSerializer
     * @param rpcServices
     * @param datastoreType
     */
    public TopoProcessingProviderImpl(SchemaService schemaService, DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, RpcServices rpcServices,
            DatastoreType datastoreType) {
        LOGGER.trace("Creating TopoProcessingProvider");
        Preconditions.checkNotNull(schemaService, "SchemaService can't be null");
        Preconditions.checkNotNull(dataBroker, "DOMDataBroker can't be null");
        Preconditions.checkNotNull(nodeSerializer, "BindingNormalizedNodeSerializer can't be null");
        Preconditions.checkNotNull(rpcServices.getRpcService(), "RpcService can't be null");
        Preconditions.checkNotNull(rpcServices.getRpcProviderService(), "RpcProviderService can't be null");
        Preconditions.checkNotNull(datastoreType, "DatastoreType can't be null");
        this.schemaService = schemaService;
        this.dataBroker = dataBroker;
        schemaHolder = new GlobalSchemaContextHolder(schemaService.getGlobalContext());
        topologyRequestListener = new TopologyRequestListener(dataBroker, nodeSerializer, schemaHolder, rpcServices);
        topologyRequestListener.setDatastoreType(datastoreType);
    }

    @Override
    public void startup() {
        schemaContextListenerRegistration =
                schemaService.registerSchemaContextListener(new GlobalSchemaContextListener(schemaHolder));
        registerTopologyRequestListener();
    }

    @Override
    public void close() throws Exception {
        LOGGER.trace("TopoProcessingProvider - close()");
        schemaContextListenerRegistration.close();
        topologyRequestListenerRegistration.close();
    }

    private void registerTopologyRequestListener() {
        LOGGER.debug("Registering Topology Request Listener");
        topologyRequestListenerRegistration =
                dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifiers.TOPOLOGY_IDENTIFIER, topologyRequestListener, DataChangeScope.ONE);
    }

}
