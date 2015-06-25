/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
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

import com.google.common.base.Optional;

/**
 * Listens on underlay topology changes
 * @author matus.marko
 */
public class UnderlayTopologyListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayTopologyListener.class);

    public enum RequestAction {
        CREATE, UPDATE, DELETE
    }

    private TopologyOperator operator;
    private YangInstanceIdentifier pathIdentifier;
    private String underlayTopologyId;
    private YangInstanceIdentifier itemIdentifier;
    private YangInstanceIdentifier itemIdIdentifier;
    private QName itemQName;
    private CorrelationItemEnum correlationItem;

    /**
     * Default constructor
     * @param operator processes received notifications (aggregates them)
     * @param underlayTopologyId underlay topology identifier
     * @param pathIdentifier identifies leaf (node), which aggregation / filtering will be based on
     * @param correlationItem can be either Node or Link or TerminationPoint
     */
    public UnderlayTopologyListener(TopologyOperator operator, String underlayTopologyId,
            YangInstanceIdentifier pathIdentifier, CorrelationItemEnum correlationItem) {
        this.operator = operator;
        this.underlayTopologyId = underlayTopologyId;
        this.pathIdentifier = pathIdentifier;
        this.correlationItem = correlationItem;
        this.itemIdentifier = InstanceIdentifiers.buildItemIdentifier(YangInstanceIdentifier.builder(), correlationItem);
        this.itemIdIdentifier = InstanceIdentifiers.buildItemIdIdentifier(correlationItem);
        this.itemQName = TopologyQNames.buildItemQName(correlationItem);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DataChangeEvent received: {}", change);
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
        Map<YangInstanceIdentifier, UnderlayItem> resultEntries = new HashMap<>();
        Iterator<Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            if (entry.getValue() instanceof MapEntryNode && entry.getValue().getNodeType().equals(itemQName)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Processing entry: {}", entry.getValue());
                }
                Optional<NormalizedNode<?,?>> itemWithItemId =
                        NormalizedNodes.findNode(entry.getValue(), itemIdIdentifier);
                String itemId;
                if (itemWithItemId.isPresent()) {
                    LeafNode<?> itemIdLeafNode = (LeafNode<?>) itemWithItemId.get();
                    itemId = itemIdLeafNode.getValue().toString();
                } else {
                    throw new IllegalStateException("item-id was not found in: " + entry.getValue());
                }
                UnderlayItem underlayItem = null;
                if (operator instanceof TopologyAggregator) {
                    // AGGREGATION
                    LOGGER.debug("Finding node/link/termination point: {}", pathIdentifier);
                    Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry.getValue(), pathIdentifier);
                    if (node.isPresent()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Found node: {}", node.get());
                        }
                        LeafNode<?> leafnode = (LeafNode<?>) node.get();
                        underlayItem = new UnderlayItem(entry.getValue(), leafnode, underlayTopologyId, itemId,
                                correlationItem);
                    } else {
                        continue;
                    }
                } else {
                    // FILTRATION
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
            if (! (lastPathArgument instanceof AugmentationIdentifier) &&
                    lastPathArgument.getNodeType().equals(itemQName) &&
                    ! lastPathArgument.equals(itemIdentifier.getLastPathArgument())) {
                identifiers.add(identifierOperational);
            }
        }
        operator.processRemovedChanges(identifiers, underlayTopologyId);
    }
}
