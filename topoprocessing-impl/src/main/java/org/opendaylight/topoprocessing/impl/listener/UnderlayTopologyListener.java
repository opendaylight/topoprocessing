/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;


/**
 * Listens on underlay topology changes
 * @author matus.marko
 */
public class UnderlayTopologyListener implements DOMDataTreeChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayTopologyListener.class);
    private final DOMDataBroker domDataBroker;

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
    public void onDataTreeChanged(Collection<DataTreeCandidate> dataTreeCandidates) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("OnDataTreeChanged event, with data tree candidates: {}", dataTreeCandidates);
        }
        Iterator<DataTreeCandidate> iterator = dataTreeCandidates.iterator();
        while (iterator.hasNext()) {
            DataTreeCandidate dataTreeCandidate = iterator.next();
            Iterator<DataTreeCandidateNode> iteratorChildNodes = dataTreeCandidate.getRootNode().getChildNodes().iterator();
            while (iteratorChildNodes.hasNext()) {
                DataTreeCandidateNode dataTreeCandidateNode = iteratorChildNodes.next();
                ModificationType modificationType = dataTreeCandidateNode.getModificationType();
                if (modificationType.equals(ModificationType.WRITE)
                        || modificationType.equals(ModificationType.SUBTREE_MODIFIED) ) {
                    if (dataTreeCandidateNode.getDataAfter().isPresent()) {
                        proceedChangeRequest(itemIdentifier.node(dataTreeCandidateNode.getIdentifier()),
                                dataTreeCandidateNode.getDataAfter(),
                                modificationType);
                     }
                } else if (modificationType.equals(ModificationType.DELETE)) {
                    proceedDeletionRequest(dataTreeCandidateNode.getIdentifier());
                } else if (modificationType.equals(ModificationType.UNMODIFIED)) {
                    continue;
                }
            }
        }
        LOGGER.debug("DataTreeChanged event processed");
    }

    /**
     * @param identifier
     * @param dataAfter
     * @param modificationType
     */
    private void proceedChangeRequest(YangInstanceIdentifier identifier,
            Optional<NormalizedNode<?, ?>> dataAfter, ModificationType modificationType) {
        Map<YangInstanceIdentifier, UnderlayItem> resultEntries = new HashMap<>();
        NormalizedNode<?, ?> entry = dataAfter.get();
        if ((entry instanceof MapEntryNode) && entry.getNodeType().equals(itemQName)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Processing entry: {}", entry);
            }
            Optional<NormalizedNode<?,?>> itemWithItemId =
                    NormalizedNodes.findNode(entry, relativeItemIdIdentifier);
            String itemId;
            if (itemWithItemId.isPresent()) {
                LeafNode<?> itemIdLeafNode = (LeafNode<?>) itemWithItemId.get();
                itemId = itemIdLeafNode.getValue().toString();
            } else {
                throw new IllegalStateException("item-id was not found in: " + entry);
            }
            UnderlayItem underlayItem = null;
            // in case that operator is instance of TopologyAggregator or PreAggregationFiltrator
            // but not TerminationPointAggregator
            if (pathIdentifier != null) {
                // AGGREGATION
                LeafNode<?> leafnode = null;
                LOGGER.debug("Finding leafnode: {}", pathIdentifier);
                Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry, pathIdentifier);
                if (node.isPresent()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Found node: {}", node.get());
                    }
                    leafnode = (LeafNode<?>) node.get();
                    underlayItem = new UnderlayItem(entry, leafnode, underlayTopologyId, itemId,
                            correlationItem);
                } else {
                    return;
                }
            } else {
                // FILTRATION or opendaylight-inventory model is used - doesn't contain leafNode
                // or Termination-point aggregation
                underlayItem = new UnderlayItem(entry, null, underlayTopologyId, itemId,
                        correlationItem);
            }
            resultEntries.put(identifier, underlayItem);
            LOGGER.debug("underlayItem created");
        }
        if (! resultEntries.isEmpty()) {
            if (modificationType == ModificationType.WRITE) {
                operator.processCreatedChanges(resultEntries, underlayTopologyId);
            } else if (modificationType == ModificationType.SUBTREE_MODIFIED) {
                operator.processUpdatedChanges(resultEntries, underlayTopologyId);
            }
        }
    }

    private void proceedDeletionRequest(PathArgument pathArgument) {
        List<YangInstanceIdentifier> identifiers = new ArrayList<>();
        if (! (pathArgument instanceof AugmentationIdentifier)
                && pathArgument.getNodeType().equals(itemQName)
                && ! pathArgument.equals(itemIdentifier.getLastPathArgument())) {
            identifiers.add(itemIdentifier.node(pathArgument));
        }
        operator.processRemovedChanges(identifiers, underlayTopologyId);
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
