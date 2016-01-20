/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Iterator;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
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


/**
 * Listens on underlay topology changes
 * @author matus.marko
 */
public abstract class UnderlayTopologyListener implements DOMDataTreeChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayTopologyListener.class);
    protected final PingPongDataBroker dataBroker;

    private TopologyOperator operator;
    private YangInstanceIdentifier pathIdentifier;
    protected String underlayTopologyId;
    protected YangInstanceIdentifier itemIdentifier;
    protected YangInstanceIdentifier relativeItemIdIdentifier;
    protected QName itemQName;
    protected CorrelationItemEnum correlationItem;

    /**
     * Default constructor
     * @param dataBroker DOM Data Broker
     * @param underlayTopologyId underlay topology identifier
     * @param correlationItem can be either Node or Link or TerminationPoint
     */
    public UnderlayTopologyListener(PingPongDataBroker dataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        this.dataBroker = dataBroker;
        this.underlayTopologyId = underlayTopologyId;
        this.correlationItem = correlationItem;
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
                if ((modificationType.equals(ModificationType.WRITE)
                        || modificationType.equals(ModificationType.SUBTREE_MODIFIED))
                        && dataTreeCandidateNode.getDataAfter().isPresent()) {
                    proceedChangeRequest(itemIdentifier.node(dataTreeCandidateNode.getIdentifier()),
                            dataTreeCandidateNode.getDataAfter().get(),
                            modificationType);
                } else if (modificationType.equals(ModificationType.DELETE)) {
                    proceedDeletionRequest(dataTreeCandidateNode.getIdentifier());
                } else if (modificationType.equals(ModificationType.UNMODIFIED)) {
                    continue;
                }
            }
        }
        LOGGER.debug("DataTreeChanged event processed");
    }

    private void proceedChangeRequest(YangInstanceIdentifier identifier,
            NormalizedNode<?,?> entry, ModificationType modificationType) {
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
                if (correlationItem == CorrelationItemEnum.TerminationPoint) {
                    underlayItem = new UnderlayItem(entry, null, underlayTopologyId, itemId,
                            correlationItem);
                } else {
                    LOGGER.debug("Finding target field");
                    Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry, pathIdentifier);
                    if (node.isPresent()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Found target field: {}", node.get());
                        }
                        leafnode = (LeafNode<?>) node.get();
                        underlayItem = new UnderlayItem(entry, leafnode, underlayTopologyId, itemId,
                                correlationItem);
                    } else {
                        return;
                    }
                }
            } else {
                // FILTRATION or opendaylight-inventory model is used - doesn't contain leafNode
                // or Termination-point aggregation
                underlayItem = new UnderlayItem(entry, null, underlayTopologyId, itemId,
                        correlationItem);
            }
            LOGGER.debug("underlayItem created");
            if (modificationType.equals(ModificationType.WRITE)) {
                operator.processCreatedChanges(identifier, underlayItem, underlayTopologyId);
            } else if (modificationType.equals(ModificationType.SUBTREE_MODIFIED)) {
                operator.processUpdatedChanges(identifier, underlayItem, underlayTopologyId);
            }
        }
    }

    private void proceedDeletionRequest(PathArgument pathArgument) {
        if (! (pathArgument instanceof AugmentationIdentifier)
                && pathArgument.getNodeType().equals(itemQName)
                && ! pathArgument.equals(itemIdentifier.getLastPathArgument())) {
            operator.processRemovedChanges(itemIdentifier.node(pathArgument), underlayTopologyId);
        }
    }

    /**
     * @param operator processes received notifications (aggregates / filters them)
     */
    public void setOperator(TopologyOperator operator) {
        this.operator = operator;
    }

    public TopologyOperator getOperator() {
        return this.operator;
    }

    /**
     * @param pathIdentifier identifies leaf (node), which aggregation / filtering will be based on
     */
    public void setPathIdentifier(YangInstanceIdentifier pathIdentifier) {
        this.pathIdentifier = pathIdentifier;
    }

    public String getUnderlayTopologyId() {
        return underlayTopologyId;
    }

    public CorrelationItemEnum getCorrelationItem() {
        return correlationItem;
    }

}
