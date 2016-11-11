/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.GlobalSchemaContextListener;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
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

    private final List<ListenerRegistration<DOMDataTreeChangeListener>> topologyRequestListenerRegistrations;
    private GlobalSchemaContextHolder schemaHolder;
    private final Map<Class<? extends Model>, ModelAdapter> modelAdapters;
    private final List<TopologyRequestListener> listeners;

    // blueprint autowired fields
    //access to data store
    private DOMDataBroker domDataBroker;
    private Broker broker;
    private DOMRpcService domRpcService;
    private DOMRpcProviderService domRpcProviderService;
    //provides schema context for lookup in models
    private SchemaService schemaService;
    //translates BindingIndependent objects to BindingAware objects (used for Topology request handling)
    private BindingNormalizedNodeSerializer nodeSerializer;
    //Configures whether framework should listen on CONFIGURATION or OPERATIONAL datastore changes
    private LogicalDatastoreType dataStoreType;

    // set-up in startup method
    private ListenerRegistration<SchemaContextListener> schemaContextListenerRegistration;
    private RpcServices rpcServices; //provides rpc services needed for rpc republishing
    private DOMDataTreeChangeService domDataTreeChangeService; // service for registering listeners

    public TopoProcessingProviderImpl() {
        LOGGER.trace("Creating TopoProcessingProvider");
        listeners = new ArrayList<>();
        topologyRequestListenerRegistrations = new ArrayList<>();
        modelAdapters = new HashMap<>();
    }

    @Override
    public void startup() {
        Preconditions.checkNotNull(schemaService, "SchemaService can't be null");
        Preconditions.checkNotNull(domRpcService, "DOMRpcService can't be null");
        Preconditions.checkNotNull(domRpcProviderService, "DOMRpcProviderService can't be null");
        Preconditions.checkNotNull(domDataBroker, "DOMDataBroker can't be null");
        Preconditions.checkNotNull(nodeSerializer, "BindingNormalizedNodeSerializer can't be null");
        Preconditions.checkNotNull(dataStoreType, "DatastoreType can't be null");
        Preconditions.checkNotNull(broker, "Broker can't be null");
        this.schemaHolder = new GlobalSchemaContextHolder(schemaService.getGlobalContext());
        schemaContextListenerRegistration = schemaService
                .registerSchemaContextListener(new GlobalSchemaContextListener(schemaHolder));

        DOMDataTreeChangeService domDataTreeService = (DOMDataTreeChangeService) domDataBroker.getSupportedExtensions()
                .get(DOMDataTreeChangeService.class);
        if (domDataTreeService != null) {
            this.domDataTreeChangeService = domDataTreeService;
        } else {
            throw new IllegalArgumentException("Received DOMDataBroker instance does not provide "
                    + "DOMDataTreeChangeService functionality. Expected PingPongDataBroker or similar instance,"
                    + " received toString(): " + domDataBroker);
        }

        rpcServices = new RpcServices(domRpcService, domRpcProviderService);
    }

    @Override
    public void close() throws Exception {
        LOGGER.trace("TopoProcessingProvider - close()");
        schemaContextListenerRegistration.close();
        for (ListenerRegistration<DOMDataTreeChangeListener> topologyRequestListenerRegistration :
            topologyRequestListenerRegistrations) {
            topologyRequestListenerRegistration.close();
        }
        dataStoreType = null;
    }

    @Override
    public void registerFiltratorFactory(Class<? extends FilterBase> filterType, FiltratorFactory filtratorFactory) {
        for (TopologyRequestListener listener : listeners) {
            listener.registerFiltrator(filterType, filtratorFactory);
        }
    }

    @Override
    public void unregisterFiltratorFactory(Class<? extends FilterBase> filterType) {
        for (TopologyRequestListener listener : listeners) {
            listener.unregisterFiltrator(filterType);
        }
    }

    @Override
    public void registerModelAdapter(Class<? extends Model> model, Object modelAdapter) {
        if (modelAdapter instanceof ModelAdapter) {
            ModelAdapter adapter = (ModelAdapter) modelAdapter;
            modelAdapters.put(model, adapter);
            if (model.equals(I2rsModel.class)) {
                registerTopologyRequestListener(adapter, InstanceIdentifiers.I2RS_NETWORK_IDENTIFIER);
            } else if (model.equals(NetworkTopologyModel.class)) {
                registerTopologyRequestListener(adapter, InstanceIdentifiers.TOPOLOGY_IDENTIFIER);
            }
        } else {
            throw new IllegalStateException("Incorrect type of ModelAdapter");
        }
    }

    private void registerTopologyRequestListener(ModelAdapter modelAdapter, YangInstanceIdentifier path) {
        TopologyRequestListener listener = modelAdapter.createTopologyRequestListener(domDataBroker,
                domDataTreeChangeService, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
        listener.setDatastoreType(dataStoreType);
        listeners.add(listener);
        LOGGER.debug("Registering Topology Request Listener");

        DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, path);
        topologyRequestListenerRegistrations.add(
                domDataTreeChangeService.registerDataTreeChangeListener(treeId, listener));
    }

    public DOMDataBroker getDataBroker() {
        return domDataBroker;
    }

    public void setDataBroker(DOMDataBroker dataBroker) {
        this.domDataBroker = dataBroker;
    }

    public SchemaService getSchemaService() {
        return schemaService;
    }

    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    public RpcServices getRpcServices() {
        return rpcServices;
    }

    public void setRpcServices(RpcServices rpcServices) {
        this.rpcServices = rpcServices;
    }

    public BindingNormalizedNodeSerializer getNodeSerializer() {
        return nodeSerializer;
    }

    public void setNodeSerializer(BindingNormalizedNodeSerializer nodeSerializer) {
        this.nodeSerializer = nodeSerializer;
    }

    public LogicalDatastoreType getDataStoreType() {
        return dataStoreType;
    }

    public void setDataStoreType(LogicalDatastoreType dataStoreType) {
        this.dataStoreType = dataStoreType;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public DOMDataTreeChangeService getDomDataTreeChangeService() {
        return domDataTreeChangeService;
    }

    public void setDomDataTreeChangeService(DOMDataTreeChangeService domDataTreeChangeService) {
        this.domDataTreeChangeService = domDataTreeChangeService;
    }

    public void setDomRpcService(final DOMRpcService domRpcService) {
        this.domRpcService = domRpcService;
    }

    public void setDomRpcProviderService(final DOMRpcProviderService domRpcProviderService) {
        this.domRpcProviderService = domRpcProviderService;
    }
}
