/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Class handling aggregation correlation
 * @author matus.marko
 * @author martin.uhlir
 */
public abstract class TopologyAggregator extends TopoStoreProvider implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyAggregator.class);
    private TopologyManager topologyManager;

    /**
     * @param topologyManager handles aggregated nodes from all correlations
     */
    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        this.topologyManager = topologyManager;
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries,
                                                final String topologyId) {
        LOG.trace("Processing createdChanges");
        if (createdEntries != null) {
            for (Entry<YangInstanceIdentifier, PhysicalNode> createdEntry : createdEntries.entrySet()) {
                for (TopologyStore ts : getTopologyStores()) {
                    if (ts.getId().equals(topologyId)) {
                        ts.getPhysicalNodes().put(createdEntry.getKey(), createdEntry.getValue());
                    }
                }
                createAggregatedNodes(createdEntry.getValue(), topologyId);
            }
        }
    }

    /**
     * Creates new logical node or adds new physical node into existing logical node if the condition
     * for correlation is satisfied
     * @param newNode - new physical node on which the correlation is created
     */
    private void createAggregatedNodes(PhysicalNode newNode, String topologyId) {
        for (TopologyStore ts : getTopologyStores()) {
            if ((! ts.getId().equals(topologyId)) || ts.isAggregateInside()) {
                for (Entry<YangInstanceIdentifier, PhysicalNode> topoStoreEntry : ts.getPhysicalNodes().entrySet()) {
                    PhysicalNode topoStoreNode = topoStoreEntry.getValue();
                    if (! newNode.equals(topoStoreNode) &&
                            topoStoreNode.getLeafNode().getValue().equals(newNode.getLeafNode().getValue())) {
                        // no previous aggregation on this node
                        if (topoStoreNode.getLogicalNode() == null) {
                            LOG.debug("Creating new Logical Node");
                            // create new logical node
                            List<PhysicalNode> nodesToAggregate = new ArrayList<>();
                            nodesToAggregate.add(newNode);
                            nodesToAggregate.add(topoStoreNode);
                            LogicalNode logicalNode = new LogicalNode(nodesToAggregate);
                            topoStoreNode.setLogicalNode(logicalNode);
                            newNode.setLogicalNode(logicalNode);
                            topologyManager.addLogicalNode(logicalNode);
                            return;
                        }
                        LOG.debug("Adding physical node to existing Logical Node");
                        // add new physical node into existing logical node
                        LogicalNode logicalNodeIdentifier = topoStoreNode.getLogicalNode();
                        newNode.setLogicalNode(logicalNodeIdentifier);
                        logicalNodeIdentifier.addPhysicalNode(newNode);
                        topologyManager.updateLogicalNode(logicalNodeIdentifier);
                        return;
                    }
                }
            }
        }
        if (wrapSingleNode()) {
            List<PhysicalNode> nodesToAggregate = new ArrayList<>();
            nodesToAggregate.add(newNode);
            LogicalNode logicalNode = new LogicalNode(nodesToAggregate);
            newNode.setLogicalNode(logicalNode);
            topologyManager.addLogicalNode(logicalNode);
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, final String topologyId) {
        LOG.trace("Processing removedChanges");
        for (TopologyStore ts : getTopologyStores()) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, PhysicalNode> physicalNodes = ts.getPhysicalNodes();
                if (identifiers != null) {
                    for (YangInstanceIdentifier identifier : identifiers) {
                        PhysicalNode physicalNode = physicalNodes.remove(identifier);
                        // if identifier exists in topology store
                        if (physicalNode != null) {
                            // if physical node is part of some logical node
                            removePhysicalNodeFromLogicalNode(physicalNode);
                        }
                    }
                }
            }
        }
    }

    private void removePhysicalNodeFromLogicalNode(PhysicalNode nodeToRemove) {
        LogicalNode logicalIdentifier = nodeToRemove.getLogicalNode();
        if (null != logicalIdentifier) {
            List<PhysicalNode> physicalNodes = logicalIdentifier.getPhysicalNodes();
            physicalNodes.remove(nodeToRemove);
            nodeToRemove.setLogicalNode(null);
            if (physicalNodes.size() < getMinPhysicalNodes()) {
                LOG.debug("Removing logical node");
                topologyManager.removeLogicalNode(logicalIdentifier);
            } else {
                LOG.debug("Removing physical node from logical node");
                topologyManager.updateLogicalNode(logicalIdentifier);
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries,
                                                String topologyId) {
        LOG.trace("Processing updatedChanges");
        if (updatedEntries != null) {
            for (Entry<YangInstanceIdentifier, PhysicalNode> updatedEntry : updatedEntries.entrySet()) {
                for (TopologyStore ts : getTopologyStores()) {
                    if (ts.getId().equals(topologyId)) {
                        LOG.debug("Updating logical node");
                        PhysicalNode physicalNode = ts.getPhysicalNodes().get(updatedEntry.getKey());
                        Preconditions.checkNotNull(physicalNode, "Updated physical node not found in Topology Store");
                        PhysicalNode updatedEntryValue = updatedEntry.getValue();
                        physicalNode.setNode(updatedEntryValue.getNode());
                        NormalizedNode<?, ?> leafNode = physicalNode.getLeafNode();
                        // if Leaf Node was changed
                        if (! leafNode.equals(updatedEntryValue.getLeafNode())) {
                            physicalNode.setLeafNode(updatedEntryValue.getLeafNode());
                            if (physicalNode.getLogicalNode() != null) {
                                removePhysicalNodeFromLogicalNode(physicalNode);
                            }
                            createAggregatedNodes(physicalNode, topologyId);
                        } else if (physicalNode.getLogicalNode() != null) {
                            // in case that only Node value was changed
                            topologyManager.updateLogicalNode(physicalNode.getLogicalNode());
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * @return minimal number of {@link PhysicalNode}s that must be present in {@link LogicalNode}
     */
    protected abstract int getMinPhysicalNodes();

    /**
     * @return true if a single {@link PhysicalNode} should be wrapped into {@link LogicalNode}
     */
    protected abstract boolean wrapSingleNode();
}
