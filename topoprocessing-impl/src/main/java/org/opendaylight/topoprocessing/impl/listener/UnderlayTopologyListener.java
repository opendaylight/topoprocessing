/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import org.opendaylight.topoprocessing.impl.operator.PreAggregationFiltrator;

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
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TerminationPointAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;


/**
 * Listens on underlay topology changes
 * @author matus.marko
 */
public class UnderlayTopologyListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayTopologyListener.class);
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
    private YangInstanceIdentifier itemIdentifier;
    private YangInstanceIdentifier relativeItemIdIdentifier;
    private QName itemQName;
    private CorrelationItemEnum correlationItem;

    /**
     * Default constructor
     * @param domDataBroker DOM Data Broker
     * @param underlayTopologyId underlay topology identifier
     * @param correlationItem can be either Node or Link or TerminationPoint
     */
    public UnderlayTopologyListener(DOMDataBroker domDataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        this.domDataBroker = domDataBroker;
        this.underlayTopologyId = underlayTopologyId;
        this.correlationItem = correlationItem;
        // this needs to be done because for processing TerminationPoints we need to filter Node instead of TP
        if (CorrelationItemEnum.TerminationPoint.equals(correlationItem)) {
            this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Node);
            this.itemQName = TopologyQNames.buildItemQName(CorrelationItemEnum.Node);
        } else {
            this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(correlationItem);
            this.itemQName = TopologyQNames.buildItemQName(correlationItem);
        }
        this.itemIdentifier = YangInstanceIdentifier.of(itemQName);
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
        Map<YangInstanceIdentifier, UnderlayItem> resultEntries = new HashMap<>();
        Iterator<Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            if (entry.getValue() instanceof MapEntryNode && entry.getValue().getNodeType().equals(itemQName)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Processing entry: {}", entry.getValue());
                }
                Optional<NormalizedNode<?,?>> itemWithItemId =
                        NormalizedNodes.findNode(entry.getValue(), relativeItemIdIdentifier);
                String itemId;
                if (itemWithItemId.isPresent()) {
                    LeafNode<?> itemIdLeafNode = (LeafNode<?>) itemWithItemId.get();
                    itemId = itemIdLeafNode.getValue().toString();
                } else {
                    throw new IllegalStateException("item-id was not found in: " + entry.getValue());
                }
                UnderlayItem underlayItem = null;
                // in case that operator is instance of TopologyAggregator or PreAggregationFiltrator
                // but not TerminationPointAggregator
                if (pathIdentifier != null) {
                    // AGGREGATION
                    LeafNode<?> leafnode = null;
                    LOGGER.debug("Finding leafnode: {}", pathIdentifier);
                    Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry.getValue(), pathIdentifier);
                    if (node.isPresent()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Found node: {}", node.get());
                        }
                        leafnode = (LeafNode<?>) node.get();
                        underlayItem = new UnderlayItem(entry.getValue(), leafnode, underlayTopologyId, itemId,
                                correlationItem);
                    } else {
                        continue;
                    }
                } else {
                    // FILTRATION or opendaylight-inventory model is used - doesn't contain leafNode
                    // or Termination-point aggregation
                    underlayItem = new UnderlayItem(entry.getValue(), null, underlayTopologyId, itemId,
                            correlationItem);
                }
                resultEntries.put(entry.getKey(), underlayItem);
                LOGGER.debug("underlayItem created");
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
            if (! (lastPathArgument instanceof AugmentationIdentifier)
                    && lastPathArgument.getNodeType().equals(itemQName)
                    && ! lastPathArgument.equals(itemIdentifier.getLastPathArgument())) {
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

    private static Map<YangInstanceIdentifier, NormalizedNode<?, ?>> listToMap(
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

    /**
     * @param operator processes received notifications (aggregates / filters them)
     */
    public void setOperator(TopologyOperator operator) {
        this.operator = operator;
    }

    /**
     * @param pathIdentifier identifies leaf (node), which aggregation / filtering will be based on
     */
    public void setPathIdentifier(YangInstanceIdentifier pathIdentifier) {
        this.pathIdentifier = pathIdentifier;
    }
}
