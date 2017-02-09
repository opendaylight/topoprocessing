/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.request;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.DefaultFiltrators;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens on new overlay topology requests.
 *
 * @author michal.polkorab
 */
public abstract class TopologyRequestListener implements DOMDataTreeChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyRequestListener.class);

    private final DOMDataBroker dataBroker;
    private final DOMDataTreeChangeService domDataTreeChangeService;
    protected YangInstanceIdentifier identifier = InstanceIdentifiers.TOPOLOGY_IDENTIFIER;
    private final BindingNormalizedNodeSerializer nodeSerializer;
    private final Map<YangInstanceIdentifier, TopologyRequestHandler> topoRequestHandlers = new HashMap<>();
    private final GlobalSchemaContextHolder schemaHolder;
    private final RpcServices rpcServices;
    private LogicalDatastoreType datastoreType;
    private final Map<Class<? extends FilterBase>, FiltratorFactory> filtrators;

    /**
     * Default constructor.
     * @param dataBroker        access to Datastore
     * @param nodeSerializer    translates Topology into BindingAware object - for easier handling in
     *                          TopologyRequestHandler
     * @param schemaHolder      access to SchemaContext and SchemaListener
     * @param rpcServices       rpcServices for rpc republishing
     * @param modelAdapters     list of registered model adapters
     */
    public TopologyRequestListener(DOMDataBroker dataBroker, DOMDataTreeChangeService domDataTreeChangeService,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices) {
        this.dataBroker = dataBroker;
        this.domDataTreeChangeService = domDataTreeChangeService;
        this.nodeSerializer = nodeSerializer;
        this.schemaHolder = schemaHolder;
        this.rpcServices = rpcServices;
        this.filtrators = DefaultFiltrators.getDefaultFiltrators();
        LOGGER.trace("Topology Request Listener created");
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> changes) {
        LOGGER.debug("DataTreeChange event notification received");
        for (DataTreeCandidate change : changes) {
            ModificationType rootModificationType = change.getRootNode().getModificationType();
            Collection<DataTreeCandidateNode> topologies = change.getRootNode().getChildNodes();
            for (DataTreeCandidateNode topology : topologies) {
                YangInstanceIdentifier yiid = YangInstanceIdentifier.builder(change.getRootPath())
                        .node(topology.getIdentifier()).build();
                Optional<NormalizedNode<?, ?>> dataAfter = topology.getDataAfter();// topology mapEntryNode
                switch (rootModificationType) {
                    case APPEARED:
                    case WRITE:
                        processCreatedData(dataAfter.get(), yiid);
                        break;
                    case DELETE:
                    case DISAPPEARED:
                        processRemovedData(yiid, 0);
                        break;
                    case SUBTREE_MODIFIED: // subtree of network-topology changed, i.e. a new topology entry was added
                        if (dataAfter.isPresent()) {
                            // we may have a Write or an Update of a topo
                            if (topology.getDataBefore().isPresent()) {
                                processUpdatedData(dataAfter.get(), yiid);
                            } else {
                                processCreatedData(dataAfter.get(), yiid);
                            }
                        } else {
                            processRemovedData(yiid, 0);
                        }
                        break;
                }
            }
        }
    }

    private void processCreatedData(NormalizedNode<?, ?> normalizedNode,
            YangInstanceIdentifier yangInstanceIdentifier) {
        LOGGER.debug("Processing created data changes");
        if (normalizedNode instanceof MapEntryNode && isTopology(normalizedNode)) {
            if (isTopologyRequest(normalizedNode) || isLinkCalculation(normalizedNode)) {
                Map.Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode = nodeSerializer
                        .fromNormalizedNode(identifier, normalizedNode);
                TopologyRequestHandler requestHandler = createTopologyRequestHandler(dataBroker,
                        domDataTreeChangeService, schemaHolder, rpcServices, fromNormalizedNode);
                requestHandler.setDatastoreType(datastoreType);
                requestHandler.setFiltrators(filtrators);
                requestHandler.prepareEmptyTopology();
                requestHandler.processNewRequest();
                topoRequestHandlers.put(yangInstanceIdentifier, requestHandler);

                Optional<DataContainerChild<? extends PathArgument, ?>> topologyTypes = ((MapEntryNode) normalizedNode)
                        .getChild(new NodeIdentifier(TopologyTypes.QNAME));
                if (topologyTypes.isPresent()) {
                    requestHandler.delegateTopologyTypes(topologyTypes.get());
                }
            } else {
                LOGGER.debug("Missing Correlations or Link Computation. At least one of them must be present");
            }
        }
    }

    /**
     * @param yangInstanceIdentifier removed path
     * @param timeOut time in ms to wait for remove to finish, if timeout == 0 there is no waiting
     */
    private void processRemovedData(YangInstanceIdentifier yangInstanceIdentifier, int timeOut) {
        LOGGER.debug("Processing removed data changes");
        TopologyRequestHandler topologyRequestHandler = topoRequestHandlers.remove(yangInstanceIdentifier);
        if (null != topologyRequestHandler) {
            topologyRequestHandler.processDeletionRequest(timeOut);
        }
    }

    private void processUpdatedData(NormalizedNode<?, ?> normalizedNode,
            YangInstanceIdentifier yangInstanceIdentifier) {
        LOGGER.debug("Processing updated data changes");
        if (normalizedNode instanceof MapEntryNode && isTopology(normalizedNode)) {
            if (isTopologyRequest(normalizedNode) || isLinkCalculation(normalizedNode)) {
                processRemovedData(yangInstanceIdentifier, 250);
            }
        }
        processCreatedData(normalizedNode, yangInstanceIdentifier);
    }

    public void close() {
        topoRequestHandlers.values().forEach(TopologyRequestHandler::close);
    }

    protected abstract boolean isTopology(NormalizedNode<?, ?> normalizedNode);

    protected abstract boolean isTopologyRequest(NormalizedNode<?, ?> normalizedNode);

    protected abstract boolean isLinkCalculation(NormalizedNode<?, ?> normalizedNode);

    protected abstract TopologyRequestHandler createTopologyRequestHandler(DOMDataBroker dataBroker,
            DOMDataTreeChangeService domDataTreeChangeService, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, Map.Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode);

    /**
     * @param datastoreType defines whether to use CONFIGURATION or OPERATIONAL Datastore
     */
    public void setDatastoreType(LogicalDatastoreType datastoreType) {
        this.datastoreType = datastoreType;
    }

    /**
     * For testing purposes only.
     *
     * @return Topology Request Handler List
     */
    public Map<YangInstanceIdentifier, TopologyRequestHandler> getTopoRequestHandlers() {
        return topoRequestHandlers;
    }

    /**
     * For testing purpose.
     *
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
    public void registerFiltrator(Class<? extends FilterBase> filtrator, FiltratorFactory filtratorFactory) {
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
