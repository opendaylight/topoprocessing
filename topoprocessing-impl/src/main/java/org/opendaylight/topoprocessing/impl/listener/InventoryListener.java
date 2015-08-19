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
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener.RequestAction;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author michal.polkorab
 *
 */
public class InventoryListener implements DOMDataTreeChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryListener.class);
    private static final YangInstanceIdentifier itemIdentifier = YangInstanceIdentifier.of(Node.QNAME);

    private TopologyOperator operator;
    private String topologyId;

    private YangInstanceIdentifier pathIdentifier;

    /**
     * Default constructor
     * @param underlayTopologyId underlay topology id
     */
    public InventoryListener(String underlayTopologyId) {
        this.topologyId = underlayTopologyId;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> dataTreeCandidates) {
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
                    processRemovedChanges(dataTreeCandidateNode.getIdentifier());
                } else if (modificationType.equals(ModificationType.UNMODIFIED)) {
                    continue;
                }
            }
        }
    }

    private void proceedChangeRequest(YangInstanceIdentifier identifier,
            NormalizedNode<?, ?> entry, ModificationType modificationType) {
        Map<YangInstanceIdentifier, UnderlayItem> resultEntries = new HashMap<>();
        if (entry instanceof MapEntryNode && entry.getNodeType().equals(Node.QNAME)) {
            Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry, pathIdentifier);
            if (node.isPresent()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Found node: {}", node.get());
                }
                NormalizedNode<?, ?> leafnode = node.get();
                UnderlayItem underlayItem =
                        new UnderlayItem(null, leafnode, topologyId, null, CorrelationItemEnum.Node);
                resultEntries.put(identifier, underlayItem);
            } else {
                return;
            }
        }
        if (! resultEntries.isEmpty()) {
            if (modificationType.equals(ModificationType.WRITE)) {
                operator.processCreatedChanges(resultEntries, topologyId);
            } else if (modificationType == ModificationType.SUBTREE_MODIFIED) {
                operator.processUpdatedChanges(resultEntries, topologyId);
            }
        }
    }

    /**
     * @param removedPaths identifies removed structures
     */
    private void processRemovedChanges(PathArgument pathArgument) {
        List<YangInstanceIdentifier> identifiers = new ArrayList<>();
        if (! (pathArgument instanceof AugmentationIdentifier)
                && pathArgument.getNodeType().equals(Node.QNAME)
                && ! pathArgument.equals(itemIdentifier.getLastPathArgument())) {
            identifiers.add(itemIdentifier.node(pathArgument));
        }
        operator.processRemovedChanges(identifiers, topologyId);
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
