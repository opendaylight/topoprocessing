/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventoryRendering.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michal.polkorab
 *
 */
public class IRInventoryListener implements DOMDataTreeChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRInventoryListener.class);
    private static final YangInstanceIdentifier itemIdentifier =
            YangInstanceIdentifier.of(Nodes.QNAME).node(Node.QNAME);

    private TopologyOperator operator;
    private String topologyId;

    private YangInstanceIdentifier pathIdentifier;

    /**
     * Default constructor.
     * @param underlayTopologyId Underlay topology id
     */
    public IRInventoryListener(String underlayTopologyId) {
        this.topologyId = underlayTopologyId;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> changes) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("OnDataTreeChanged event, with data tree candidates: {}", changes);
        }
        Iterator<DataTreeCandidate> iterator = changes.iterator();
        while (iterator.hasNext()) {
            DataTreeCandidate dataTreeCandidate = iterator.next();
            Iterator<DataTreeCandidateNode> iteratorChildNodes =
                    dataTreeCandidate.getRootNode().getChildNodes().iterator();
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
        LOGGER.debug("DataTreeChanged event processed");
    }

    private void proceedChangeRequest(YangInstanceIdentifier identifier, NormalizedNode<?, ?> entry,
            ModificationType requestAction) {
        if (entry instanceof MapEntryNode && entry.getNodeType().equals(Node.QNAME)) {
            Map<Integer, NormalizedNode<?, ?>> leafNode = new HashMap<>(1);
            leafNode.put(0, entry);
            UnderlayItem underlayItem =
                    new UnderlayItem(null, leafNode, topologyId, null, CorrelationItemEnum.Node);
            if (requestAction == ModificationType.WRITE) {
                operator.processCreatedChanges(identifier, underlayItem, topologyId);
            } else if (requestAction == ModificationType.SUBTREE_MODIFIED) {
                operator.processUpdatedChanges(identifier, underlayItem, topologyId);
            }
        }
    }

    /**
     * Process removed changes identified by path argument.
     * @param removedPaths identifies removed structures.
     */
    private void processRemovedChanges(PathArgument pathArgument) {
        if (! (pathArgument instanceof AugmentationIdentifier)
                && pathArgument.getNodeType().equals(Node.QNAME)
                && ! pathArgument.equals(itemIdentifier.getLastPathArgument())) {
            operator.processRemovedChanges(itemIdentifier.node(pathArgument), topologyId);
        }
    }

    /**
     * Set topology operator.
     * @param operator Processes received notifications (aggregates / filters them).
     */
    public void setOperator(TopologyOperator operator) {
        this.operator = operator;
    }

    /**
     * Set path identifier.
     * @param pathIdentifier Identifies leaf (node), which aggregation / filtering will be based on.
     */
    public void setPathIdentifier(YangInstanceIdentifier pathIdentifier) {
        this.pathIdentifier = pathIdentifier;
    }
}
