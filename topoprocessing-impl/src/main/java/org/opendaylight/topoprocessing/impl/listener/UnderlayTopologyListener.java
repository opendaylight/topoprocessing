/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import java.util.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

/**
 * Listens on underlay topology changes
 * @author matus.marko
 */
public class UnderlayTopologyListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayTopologyListener.class);
    private static final YangInstanceIdentifier NODE_IDENTIFIER = YangInstanceIdentifier.builder().node(Node.QNAME).build();
    private static final YangInstanceIdentifier NODE_ID_IDENTIFIER = YangInstanceIdentifier.of(TopologyQNames.NETWORK_NODE_ID_QNAME);
    private final DOMDataBroker domDataBroker;
    private final DatastoreType datastoreType;
    private volatile CountDownLatch lastLatch;
    private AtomicReferenceFieldUpdater<UnderlayTopologyListener, CountDownLatch> updater =
            AtomicReferenceFieldUpdater.newUpdater(UnderlayTopologyListener.class, CountDownLatch.class, "lastLatch");

    public enum RequestAction {
        CREATE, UPDATE, DELETE
    }

    private TopologyOperator operator;
    private YangInstanceIdentifier pathIdentifier;
    private String underlayTopologyId;

    /**
     * Default constructor
     * @param operator processes received notifications (aggregates them)
     * @param underlayTopologyId underlay topology identifier
     * @param pathIdentifier identifies leaf (node), which aggregation / filtering will be based on
     */
    public UnderlayTopologyListener(DOMDataBroker domDataBroker, TopologyOperator operator, String underlayTopologyId,
            YangInstanceIdentifier pathIdentifier, DatastoreType datastoreType) {
        this.domDataBroker = domDataBroker;
        this.operator = operator;
        this.underlayTopologyId = underlayTopologyId;
        this.pathIdentifier = pathIdentifier;
        this.datastoreType = datastoreType;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DataChangeEvent received: {}", change);
        }
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch waitLatch = updater.getAndSet(this, latch);
        if (0 != waitLatch.getCount()) {
            try {
                waitLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Exception while waiting for the latch", e);
            }
        }
        if (null == waitLatch) {
            throw new RuntimeException("Read data first");
        }
        if (! change.getCreatedData().isEmpty()) {
            LOGGER.debug("Processing createdData");
            this.proceedChangeRequest(change.getCreatedData(), RequestAction.CREATE);
        }
        if (! change.getUpdatedData().isEmpty()) {
            LOGGER.debug("Processing updatedData");
            this.proceedChangeRequest(change.getUpdatedData(), RequestAction.UPDATE);
        }
        if (! change.getRemovedPaths().isEmpty()) {
            LOGGER.debug("Processing removedData");
            this.proceedDeletionRequest(change.getRemovedPaths());
        }
        LOGGER.debug("DataChangeEvent processed");
    }

    private void proceedChangeRequest(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map,
            RequestAction requestAction) {
        Map<YangInstanceIdentifier, PhysicalNode> resultEntries = new HashMap<>();
        Iterator<Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            if (entry.getValue() instanceof MapEntryNode && entry.getValue().getNodeType().equals(Node.QNAME)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Processing entry: {}", entry.getValue());
                }
                Optional<NormalizedNode<?,?>> nodeWithNodeId =
                        NormalizedNodes.findNode(entry.getValue(), NODE_ID_IDENTIFIER);
                String nodeId;
                if (nodeWithNodeId.isPresent()) {
                    LeafNode<?> nodeIdLeafNode = (LeafNode<?>) nodeWithNodeId.get();
                    nodeId = nodeIdLeafNode.getValue().toString();
                } else {
                    throw new IllegalStateException("node-id was not found in: " + entry.getValue());
                }
                PhysicalNode physicalNode = null;
                if (operator instanceof TopologyAggregator) {
                    // AGGREGATION
                    LOGGER.debug("Finding node: {}", pathIdentifier);
                    Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry.getValue(), pathIdentifier);
                    if (node.isPresent()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Found node: {}", node.get());
                        }
                        LeafNode<?> leafnode = (LeafNode<?>) node.get();
                        physicalNode = new PhysicalNode(entry.getValue(), leafnode, underlayTopologyId, nodeId);
                    } else {
                        continue;
                    }
                } else {
                    // FILTRATION
                    physicalNode = new PhysicalNode(entry.getValue(), null, underlayTopologyId, nodeId);
                }
                resultEntries.put(entry.getKey(), physicalNode);
                LOGGER.debug("PhysicalNode created");
            }
        }
        if (! resultEntries.isEmpty()) {
            if (requestAction == RequestAction.CREATE) {
                operator.processCreatedChanges(resultEntries, underlayTopologyId);
            } else if (requestAction == RequestAction.UPDATE) {
                operator.processUpdatedChanges(resultEntries, underlayTopologyId);
            }
        }
    }

    private void proceedDeletionRequest(Set<YangInstanceIdentifier> set) {
        List<YangInstanceIdentifier> identifiers = new ArrayList<>();
        Iterator<YangInstanceIdentifier> iterator = set.iterator();
        while (iterator.hasNext()) {
            YangInstanceIdentifier identifierOperational = iterator.next();
            PathArgument lastPathArgument = identifierOperational.getLastPathArgument();
            if (! (lastPathArgument instanceof AugmentationIdentifier) &&
                    lastPathArgument.getNodeType().equals(Node.QNAME) && 
                    ! lastPathArgument.equals(NODE_IDENTIFIER.getLastPathArgument())) {
                identifiers.add(identifierOperational);
            }
        }
        operator.processRemovedChanges(identifiers, underlayTopologyId);
    }

    public void readExistingData() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        updater.getAndSet(this, countDownLatch);

        YangInstanceIdentifier path = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId)
                .node(Node.QNAME).build();
        LogicalDatastoreType logicalDatastoreType = (DatastoreType.CONFIGURATION == datastoreType) ?
                LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL;
        DOMDataReadOnlyTransaction transaction = domDataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
                transaction.read(logicalDatastoreType, path);
        Futures.addCallback(future, new FutureCallback<Optional<NormalizedNode<?, ?>>>() {
            @Override
            public void onSuccess(Optional<NormalizedNode<?, ?>> result) {
                LOGGER.trace("Read from dataStore: {}", result);
                proceedChangeRequest(listToMap((Collection) result.get().getValue(), underlayTopologyId),
                        RequestAction.CREATE);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.warn("DataStore read failed");
            }
        });
        countDownLatch.countDown();
    }

    private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> listToMap(
            Collection nodes, String underlayTopologyId) {
        InstanceIdentifierBuilder nodeYiidBuilder  = YangInstanceIdentifier
                .builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId)
                .node(Node.QNAME);
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = new HashMap<>();
        for (MapEntryNode node : (Collection<MapEntryNode>) nodes) {
            map.put(nodeYiidBuilder.nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, node.getIdentifier())
                    .build(), node);
        }
        return map;
    }
}
