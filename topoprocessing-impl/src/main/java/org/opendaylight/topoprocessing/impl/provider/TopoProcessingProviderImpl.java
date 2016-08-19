/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.provider;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.NoOpTopoprocessingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michal.polkorab
 *
 */
public class TopoProcessingProviderImpl implements TopoProcessingProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopoProcessingProviderImpl.class);

    private final List<ListenerRegistration<DOMDataChangeListener>> topologyRequestListenerRegistrations;
    private GlobalSchemaContextHolder schemaHolder;
    private final Map<Class<? extends Model>, ModelAdapter> modelAdapters;
    private final List<TopologyRequestListener> listeners;

    // blueprint autowired fields
    private DOMDataBroker dataBroker;
    private Broker broker;
    private SchemaService schemaService;
    private BindingNormalizedNodeSerializer nodeSerializer;
    private DatastoreType dataStoreType;

    // set-up in startup method
    private ListenerRegistration<SchemaContextListener> schemaContextListenerRegistration;
    private RpcServices rpcServices;

    /**
     * @param schemaService
     *            provides schema context for lookup in models
     * @param dataBroker
     *            access to data store
     * @param nodeSerializer
     *            translates BindingIndependent objects to BindingAware objects
     *            (used for Topology request handling)
     * @param rpcServices
     *            provides rpc services needed for rpc republishing
     * @param datastoreType
     *            Configures if framework should listen on CONFIGURATION or
     *            OPERATIONAL datastore changes
     */
    public TopoProcessingProviderImpl() {
        LOGGER.trace("Creating TopoProcessingProvider");
        listeners = new ArrayList<>();
        topologyRequestListenerRegistrations = new ArrayList<>();
        modelAdapters = new HashMap<>();
    }

    @Override
    public void startup() {
        Preconditions.checkNotNull(schemaService, "SchemaService can't be null");
        Preconditions.checkNotNull(dataBroker, "DOMDataBroker can't be null");
        Preconditions.checkNotNull(nodeSerializer, "BindingNormalizedNodeSerializer can't be null");
        Preconditions.checkNotNull(dataStoreType, "DatastoreType can't be null");
        Preconditions.checkNotNull(broker, "Broker can't be null");

        this.schemaHolder = new GlobalSchemaContextHolder(schemaService.getGlobalContext());
        schemaContextListenerRegistration = schemaService
                .registerSchemaContextListener(new GlobalSchemaContextListener(schemaHolder));

        ProviderSession session = broker.registerProvider(new NoOpTopoprocessingProvider());
        DOMRpcService rpcService = session.getService(DOMRpcService.class);
        DOMRpcProviderService rpcProviderService = session.getService(DOMRpcProviderService.class);
        rpcServices = new RpcServices(rpcService, rpcProviderService);
    }

    @Override
    public void close() throws Exception {
        LOGGER.trace("TopoProcessingProvider - close()");
        schemaContextListenerRegistration.close();
        for (ListenerRegistration<DOMDataChangeListener> topologyRequestListenerRegistration : topologyRequestListenerRegistrations) {
            topologyRequestListenerRegistration.close();
        }
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
        TopologyRequestListener listener = modelAdapter.createTopologyRequestListener(dataBroker, nodeSerializer,
                schemaHolder, rpcServices, modelAdapters);
        listener.setDatastoreType(dataStoreType);
        listeners.add(listener);
        LOGGER.debug("Registering Topology Request Listener");
        topologyRequestListenerRegistrations.add(dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, path, listener, DataChangeScope.SUBTREE));
    }

    public DOMDataBroker getDataBroker() {
        return dataBroker;
    }

    public void setDataBroker(DOMDataBroker dataBroker) {
        this.dataBroker = dataBroker;
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

    public DatastoreType getDataStoreType() {
        return dataStoreType;
    }

    public void setDataStoreType(DatastoreType dataStoreType) {
        this.dataStoreType = dataStoreType;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

}
