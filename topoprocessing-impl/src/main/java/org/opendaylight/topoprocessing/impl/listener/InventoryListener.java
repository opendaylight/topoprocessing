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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michal.polkorab
 *
 */
public class InventoryListener implements DOMDataTreeChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryListener.class);
    private static final YangInstanceIdentifier itemIdentifier =
            YangInstanceIdentifier.of(Nodes.QNAME).node(Node.QNAME);
    private TopologyOperator operator;
    private String topologyId;
    private Map<Integer, YangInstanceIdentifier> pathIdentifiers;
    private CorrelationItemEnum correlationItem;

    /**
     * Default constructor
     * @param underlayTopologyId underlay topology id
     */
    public InventoryListener(String underlayTopologyId) {
        this.topologyId = underlayTopologyId;
    }

    public InventoryListener(String underlayTopologyId, CorrelationItemEnum correlationItem) {
        this(underlayTopologyId);
        this.correlationItem = correlationItem;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> dataTreeCandidates) {
        Iterator<DataTreeCandidate> iterator = dataTreeCandidates.iterator();
        while (iterator.hasNext()) {
            DataTreeCandidate dataTreeCandidate = iterator.next();
            Iterator<DataTreeCandidateNode> iteratorChildNodes =
                    dataTreeCandidate.getRootNode().getChildNodes().iterator();
            while (iteratorChildNodes.hasNext()) {
                DataTreeCandidateNode dataTreeCandidateNode = iteratorChildNodes.next();
                ModificationType modificationType = dataTreeCandidateNode.getModificationType();
                if ((modificationType.equals(ModificationType.WRITE) ||
                        modificationType.equals(ModificationType.SUBTREE_MODIFIED))
                        && dataTreeCandidateNode.getDataAfter().isPresent()) {
                    boolean updated = dataTreeCandidateNode.getDataBefore().isPresent() ||
                            modificationType.equals(ModificationType.SUBTREE_MODIFIED);
                    proceedChangeRequest(itemIdentifier.node(dataTreeCandidateNode.getIdentifier()),
                            dataTreeCandidateNode.getDataAfter().get(), updated);
                } else if (modificationType.equals(ModificationType.DELETE)) {
                    processRemovedChanges(dataTreeCandidateNode.getIdentifier());
                }
            }
        }
    }

    private void proceedChangeRequest(YangInstanceIdentifier identifier,
            NormalizedNode<?, ?> entry, boolean updated) {
        if (entry instanceof MapEntryNode && entry.getNodeType().equals(Node.QNAME)) {
            Map<Integer, NormalizedNode<?, ?>> targetFields = fillTargetFields(entry);
            if (!targetFields.isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Found target fields: {}", targetFields);
                }
                UnderlayItem underlayItem =
                        new UnderlayItem(null, targetFields, topologyId, null, CorrelationItemEnum.Node);
                if (!updated) {
                    operator.processCreatedChanges(identifier, underlayItem, topologyId);
                } else {
                    operator.processUpdatedChanges(identifier, underlayItem, topologyId);
                }
            }
        }
    }

    private Map<Integer, NormalizedNode<?, ?>> fillTargetFields(NormalizedNode<?, ?> entry) {
        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(pathIdentifiers.size());
        if (correlationItem.equals(CorrelationItemEnum.TerminationPoint)) {
            targetFields.put(0, entry);
        } else {
            for (Entry<Integer, YangInstanceIdentifier> pathIdentifierEntry : pathIdentifiers.entrySet()) {
                Optional<NormalizedNode<?, ?>> targetFieldOpt =
                        NormalizedNodes.findNode(entry, pathIdentifierEntry.getValue());
                if (targetFieldOpt.isPresent()) {
                    targetFields.put(pathIdentifierEntry.getKey(), targetFieldOpt.get());
                }
            }
        }
        return targetFields;
    }

    /**
     * @param removedPaths identifies removed structures
     */
    private void processRemovedChanges(PathArgument pathArgument) {
        if (! (pathArgument instanceof AugmentationIdentifier)
                && pathArgument.getNodeType().equals(Node.QNAME)
                && ! pathArgument.equals(itemIdentifier.getLastPathArgument())) {
            operator.processRemovedChanges(itemIdentifier.node(pathArgument), topologyId);
        }
    }

    /**
     * @param operator processes received notifications (aggregates / filters them)
     */
    public void setOperator(TopologyOperator operator) {
        this.operator = operator;
    }

    /**
     * @param pathIdentifiers identifies leaf (node), which aggregation / filtering will be based on
     */
    public void setPathIdentifier(Map<Integer, YangInstanceIdentifier> pathIdentifiers) {
        this.pathIdentifiers = pathIdentifiers;
    }
}
