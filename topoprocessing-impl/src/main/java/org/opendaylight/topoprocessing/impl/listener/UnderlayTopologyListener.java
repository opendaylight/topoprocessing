/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import com.google.common.base.Preconditions;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.annotation.Nullable;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Listens on underlay topology changes
 * @author matus.marko
 */
public class UnderlayTopologyListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayTopologyListener.class);
    private static final YangInstanceIdentifier NODE_IDENTIFIER = YangInstanceIdentifier.builder().node(Node.QNAME).build();
    private static final YangInstanceIdentifier NODE_ID_IDENTIFIER = YangInstanceIdentifier.of(TopologyQNames.NETWORK_NODE_ID_QNAME);
    private final DOMDataBroker domDataBroker;
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
     * @param domDataBroker DOM Data Broker
     * @param operator processes received notifications (aggregates them)
     * @param underlayTopologyId underlay topology identifier
     * @param pathIdentifier identifies leaf (node), which aggregation / filtering will be based on
     */
    public UnderlayTopologyListener(DOMDataBroker domDataBroker, TopologyOperator operator, String underlayTopologyId,
            YangInstanceIdentifier pathIdentifier) {
        this.domDataBroker = domDataBroker;
        this.operator = operator;
        this.underlayTopologyId = underlayTopologyId;
        this.pathIdentifier = pathIdentifier;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DataChangeEvent received: {}", change);
        }
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch waitLatch = updater.getAndSet(this, latch);
        Preconditions.checkNotNull(waitLatch, "Read data first");
        if (0 != waitLatch.getCount()) {
            try {
                waitLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Exception while waiting for the latch", e);
            }
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
        latch.countDown();
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
                PhysicalNode physicalNode;
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

    /**
     * Reads data that were written before overlay topology request was received
     * @param path path to existing data
     * @param datastoreType datastore to read from (operational, configuration)
     */
    public void readExistingData(YangInstanceIdentifier path, DatastoreType datastoreType) {
        LOGGER.trace("Reading existing data from dataStore");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        updater.getAndSet(this, countDownLatch);
        DOMDataReadOnlyTransaction transaction = domDataBroker.newReadOnlyTransaction();
        LogicalDatastoreType logicalDatastoreType = (DatastoreType.CONFIGURATION == datastoreType) ?
                LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL;
        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
                transaction.read(logicalDatastoreType, path);
        Futures.addCallback(future, new FutureCallback<Optional<NormalizedNode<?, ?>>>() {
            @Override
            public void onSuccess(Optional<NormalizedNode<?, ?>> result) {
                if (result.isPresent()) {
                    LOGGER.debug("Existing data read");
                    Collection<NormalizedNode<?, ?>> value =
                            (Collection<NormalizedNode<?, ?>>) result.get().getValue();
                    proceedChangeRequest(listToMap(value.iterator(), underlayTopologyId), RequestAction.CREATE);
                } else {
                    LOGGER.debug("No data present. Proceeding with notifications.");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.warn("DataStore read failed - no existing data were read");
            }
        });
        countDownLatch.countDown();
    }

    private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> listToMap(
            Iterator<NormalizedNode<?, ?>> nodes, final String underlayTopologyId) {
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = Maps.uniqueIndex(nodes,
                new Function<NormalizedNode<?, ?>, YangInstanceIdentifier>() {
            @Nullable
            @Override
            public YangInstanceIdentifier apply(NormalizedNode<?, ?> node) {
                return YangInstanceIdentifier
                        .builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                        .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId)
                        .node(Node.QNAME)
                        .node(node.getIdentifier())
                        .build();
            }
        });
        return map;
    }
}
