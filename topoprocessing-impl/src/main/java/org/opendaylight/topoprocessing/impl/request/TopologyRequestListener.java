/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtrator.DefaultFiltrators;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
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

import com.google.common.base.Optional;

/**
 * Listens on new overlay topology requests
 *
 * @author michal.polkorab
 */
public class TopologyRequestListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyRequestListener.class);

    private DOMDataBroker dataBroker;
    private YangInstanceIdentifier identifier = InstanceIdentifiers.TOPOLOGY_IDENTIFIER;
    private BindingNormalizedNodeSerializer nodeSerializer;
    private Map<YangInstanceIdentifier, TopologyRequestHandler> topoRequestHandlers = new HashMap<>();
    private GlobalSchemaContextHolder schemaHolder;
    private RpcServices rpcServices;
    private DatastoreType datastoreType;
    private Map<Class<? extends FilterBase>, FiltratorFactory> filtrators;

    /**
     * Default constructor
     * @param dataBroker
     * @param nodeSerializer
     * @param schemaHolder
     * @param rpcServices
     */
    public TopologyRequestListener(DOMDataBroker dataBroker, BindingNormalizedNodeSerializer nodeSerializer,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices) {
        this.dataBroker = dataBroker;
        this.nodeSerializer = nodeSerializer;
        this.schemaHolder = schemaHolder;
        this.rpcServices = rpcServices;
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
            processRemovedData(change.getRemovedPaths());
        }
    }

    private void processCreatedData(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map) {
        LOGGER.debug("Processing created data changes");
        for(Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry : map.entrySet()) {
            YangInstanceIdentifier yangInstanceIdentifier = entry.getKey();
            NormalizedNode<?, ?> normalizedNode = entry.getValue();
            if(normalizedNode instanceof MapEntryNode && normalizedNode.getNodeType().equals(Topology.QNAME)) {
                Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode =
                        nodeSerializer.fromNormalizedNode(identifier, normalizedNode);
                Topology topology = (Topology) fromNormalizedNode.getValue();
                if (topology.getAugmentation(CorrelationAugment.class) != null) {
                    TopologyRequestHandler requestHandler =
                            new TopologyRequestHandler(dataBroker, schemaHolder, rpcServices);
                    requestHandler.setDatastoreType(datastoreType);
                    requestHandler.setFiltrators(filtrators);
                    requestHandler.processNewRequest(topology);
                    topoRequestHandlers.put(yangInstanceIdentifier,requestHandler);

                    Optional<DataContainerChild<? extends PathArgument, ?>> topologyTypes =
                            ((MapEntryNode) normalizedNode).getChild(new NodeIdentifier(TopologyTypes.QNAME));
                    if (topologyTypes.isPresent()) {
                        requestHandler.delegateTopologyTypes(topologyTypes.get());
                    }
                }
            }
        }
    }

    private void processRemovedData(Set<YangInstanceIdentifier> removedPaths) {
        LOGGER.debug("Processing removed data changes");
        for (YangInstanceIdentifier yangInstanceIdentifier : removedPaths) {
            TopologyRequestHandler topologyRequestHandler = topoRequestHandlers.remove(yangInstanceIdentifier);
            if (null != topologyRequestHandler) {
                topologyRequestHandler.processDeletionRequest();
            }
        }
    }

    /**
     * @param datastoreType
     */
    public void setDatastoreType(DatastoreType datastoreType) {
        this.datastoreType = datastoreType;
    }

    /**
     * For testing purposes only
     * @return Topology Request Handler List
     */
    public Map<YangInstanceIdentifier, TopologyRequestHandler> getTopoRequestHandlers() {
        return topoRequestHandlers;
    }

    /** for testing purpose */
    public Map<Class<? extends FilterBase>, FiltratorFactory> getFiltrators() {
        return filtrators;
    }

    /**
     * @param filtrator
     * @param filtratorFactory
     */
    public void registerFiltrator(Class<? extends FilterBase> filtrator,
            FiltratorFactory filtratorFactory) {
        filtrators.put(filtrator, filtratorFactory);
    }

    /**
     * @param filtrator
     */
    public void unregisterFiltrator(Class<? extends FilterBase> filtrator) {
        filtrators.remove(filtrator);
    }
}
