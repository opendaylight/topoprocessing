/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.request;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.DefaultFiltrators;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens on new overlay topology requests.
 *
 * @author michal.polkorab
 */
public abstract class TopologyRequestListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyRequestListener.class);

    private DOMDataBroker dataBroker;
    protected YangInstanceIdentifier identifier = InstanceIdentifiers.TOPOLOGY_IDENTIFIER;
    private BindingNormalizedNodeSerializer nodeSerializer;
    private Map<YangInstanceIdentifier, TopologyRequestHandler> topoRequestHandlers = new HashMap<>();
    private GlobalSchemaContextHolder schemaHolder;
    private RpcServices rpcServices;
    private DatastoreType datastoreType;
    private Map<Class<? extends FilterBase>, FiltratorFactory> filtrators;
    private Map<Class<? extends Model>, ModelAdapter> modelAdapters;

    /**
     * Default constructor.
     * @param dataBroker        access to Datastore
     * @param nodeSerializer    translates Topology into BindingAware object - for easier handling in
     *                          TopologyRequestHandler
     * @param schemaHolder      access to SchemaContext and SchemaListener
     * @param rpcServices       rpcServices for rpc republishing
     * @param modelAdapters     list of registered model adapters
     */
    public TopologyRequestListener(DOMDataBroker dataBroker, BindingNormalizedNodeSerializer nodeSerializer,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
            Map<Class<? extends Model>, ModelAdapter> modelAdapters) {
        this.dataBroker = dataBroker;
        this.nodeSerializer = nodeSerializer;
        this.schemaHolder = schemaHolder;
        this.rpcServices = rpcServices;
        this.modelAdapters = modelAdapters;
        this.filtrators = DefaultFiltrators.getDefaultFiltrators();
        LOGGER.trace("Topology Request Listener created");
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        LOGGER.debug("DataChange event notification received");

        if (! change.getCreatedData().isEmpty()) {
            processCreatedData(change.getCreatedData());
        }
        if (! change.getRemovedPaths().isEmpty()) {
            processRemovedData(change.getRemovedPaths(),0);
        }
        if (! change.getUpdatedData().isEmpty()) {
            processUpdatedData(change.getUpdatedData());
        }
    }

    private void processCreatedData(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map) {
        LOGGER.debug("Processing created data changes");

        for (Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry : map.entrySet()) {
            YangInstanceIdentifier yangInstanceIdentifier = entry.getKey();
            NormalizedNode<?, ?> normalizedNode = entry.getValue();
            if (normalizedNode instanceof MapEntryNode && isTopology(normalizedNode)) {
                if (isTopologyRequest(normalizedNode) || isLinkCalculation(normalizedNode)) {
                    Map.Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode =
                            nodeSerializer.fromNormalizedNode(identifier, normalizedNode);
                    TopologyRequestHandler requestHandler =
                            createTopologyRequestHandler(dataBroker, schemaHolder, rpcServices, fromNormalizedNode);
                    requestHandler.setDatastoreType(datastoreType);
                    requestHandler.setFiltrators(filtrators);
                    requestHandler.setModelAdapters(modelAdapters);
                    requestHandler.processNewRequest();
                    topoRequestHandlers.put(yangInstanceIdentifier,requestHandler);

                    Optional<DataContainerChild<? extends PathArgument, ?>> topologyTypes =
                            ((MapEntryNode) normalizedNode).getChild(new NodeIdentifier(TopologyTypes.QNAME));
                    if (topologyTypes.isPresent()) {
                        requestHandler.delegateTopologyTypes(topologyTypes.get());
                    }
                } else {
                    LOGGER.debug("Missing Correlations or Link Computation. At least one of them must be present");
                }
            }
        }
    }

    /**
     * @param removedPaths set of removed paths
     * @param timeOut time in ms to wait for remove to finish, if timeout == 0 there is no waiting
     */
    private void processRemovedData(Set<YangInstanceIdentifier> removedPaths,int timeOut) {
        LOGGER.debug("Processing removed data changes");
        for (YangInstanceIdentifier yangInstanceIdentifier : removedPaths) {
            TopologyRequestHandler topologyRequestHandler = topoRequestHandlers.remove(yangInstanceIdentifier);
            if (null != topologyRequestHandler) {
                topologyRequestHandler.processDeletionRequest(timeOut);
            }
        }
    }

    private void processUpdatedData(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        LOGGER.debug("Processing updated data changes");
        for (Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry : change.entrySet()) {
            YangInstanceIdentifier yangInstanceIdentifier = entry.getKey();
            NormalizedNode<?, ?> normalizedNode = entry.getValue();

            if (normalizedNode instanceof MapEntryNode && isTopology(normalizedNode)) {
                if (isTopologyRequest(normalizedNode) || isLinkCalculation(normalizedNode)) {
                    Set<YangInstanceIdentifier> key = new HashSet<>();
                    key.add(yangInstanceIdentifier);
                    processRemovedData(key,250);
                }
            }
        }
        processCreatedData(change);
    }

    protected abstract boolean isTopology(NormalizedNode<?,?> normalizedNode);

    protected abstract boolean isTopologyRequest(NormalizedNode<?,?> normalizedNode);

    protected abstract boolean isLinkCalculation(NormalizedNode<?, ?> normalizedNode);

    protected abstract TopologyRequestHandler createTopologyRequestHandler(DOMDataBroker dataBroker,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
            Map.Entry<InstanceIdentifier<?>,DataObject> fromNormalizedNode);

    /**
     * @param datastoreType defines whether to use CONFIGURATION or OPERATIONAL Datastore
     */
    public void setDatastoreType(DatastoreType datastoreType) {
        this.datastoreType = datastoreType;
    }

    /**
     * For testing purposes only.
     * @return Topology Request Handler List
     */
    public Map<YangInstanceIdentifier, TopologyRequestHandler> getTopoRequestHandlers() {
        return topoRequestHandlers;
    }

    /**
     * For testing purpose.
     * @return All Filtrators
     */
    public Map<Class<? extends FilterBase>, FiltratorFactory> getFiltrators() {
        return filtrators;
    }

    /**
     * Register new filtrator with its own factory.
     * @param filtrator         filter type
     * @param filtratorFactory  creates Filtrators based on Filter configuration
     */
    public void registerFiltrator(Class<? extends FilterBase> filtrator,
            FiltratorFactory filtratorFactory) {
        filtrators.put(filtrator, filtratorFactory);
    }

    /**
     * Unregister Filtrator from Listener.
     * @param filtrator unregisters FiltratorFactory for specifier filter type,
     *                  filtrators for this filter type can no longer be created
     */
    public void unregisterFiltrator(Class<? extends FilterBase> filtrator) {
        filtrators.remove(filtrator);
    }
}
