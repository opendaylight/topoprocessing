/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class TopologyAggregator implements TopologyOperator {

    private Map<YangInstanceIdentifier,LogicalNode> aggregationMap = new HashMap<>();
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private CorrelationItemEnum correlationItem;
    private List<TopologyStore> topologyStores;

    /**
     * Constructor
     * @param correlationItem
     */
    public TopologyAggregator(CorrelationItemEnum correlationItem, List<TopologyStore> topologyStores) {
        this.correlationItem = correlationItem;
        this.topologyStores = topologyStores;
    }

    /**
     * Process newly created changes
     * @param createdEntries
     * @param topologyId
     */
    public void processCreatedChanges_old(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, 
            final String topologyId) {
        if (createdEntries != null) {
            for (TopologyStore ts : topologyStores) {
                if (ts.getId().equals(topologyId)) {
                    for (Entry<YangInstanceIdentifier, PhysicalNode> createdEntry : createdEntries.entrySet()) {
                        ts.getPhysicalNodes().put(createdEntry.getKey(), createdEntry.getValue());
                    }
                } else {
                    for (Entry<YangInstanceIdentifier, PhysicalNode> createdEntry : createdEntries.entrySet()) {
                        for (Entry<YangInstanceIdentifier, PhysicalNode> entry : ts.getPhysicalNodes().entrySet()) {
                            if (createdEntry.getValue().getLeafNode().equals(entry.getValue().getLeafNode())) {
                                YangInstanceIdentifier logicalNodeIdentifier =
                                        idGenerator.getNextIdentifier(topologyId, correlationItem);
                                createdEntry.getValue().setLogicalIdentifier(logicalNodeIdentifier);
                                entry.getValue().setLogicalIdentifier(logicalNodeIdentifier);
                                List<PhysicalNode> physicalNodes = new ArrayList<>();
                                physicalNodes.add(createdEntry.getValue());
                                physicalNodes.add(entry.getValue());
                                LogicalNode logicalNode = new LogicalNode(physicalNodes);
                                aggregationMap.put(logicalNodeIdentifier, logicalNode);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Process newly created changes
     * @param createdEntries
     * @param topologyId
     */
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, 
            final String topologyId) {
        for (Entry<YangInstanceIdentifier, PhysicalNode> createdEntry : createdEntries.entrySet()) {
            for (TopologyStore ts : topologyStores) {
                if (ts.getId().equals(topologyId)) {
                    ts.getPhysicalNodes().put(createdEntry.getKey(), createdEntry.getValue());
                }
            }
            createAggregatedNodes(createdEntry.getValue(), topologyId);
        }
    }

    /**
     * @param newNode
     * @param topologyId
     */
    private void createAggregatedNodes(PhysicalNode newNode,String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (!ts.getId().equals(topologyId)) {
                for (Entry<YangInstanceIdentifier, PhysicalNode> topoStoreEntry : ts.getPhysicalNodes().entrySet()) {
                    PhysicalNode topoStoreNode = topoStoreEntry.getValue();
                    if (topoStoreNode.getLeafNode().equals(newNode.getLeafNode())) {
                        if (topoStoreNode.getLogicalIdentifier() == null) {
                            // create new logical node
                            List<PhysicalNode> nodesToAggregate = new ArrayList<>();
                            nodesToAggregate.add(newNode);
                            nodesToAggregate.add(topoStoreNode);
                            LogicalNode logicalNode = new LogicalNode(nodesToAggregate);
                            YangInstanceIdentifier logicalNodeIdentifier =
                                    idGenerator.getNextIdentifier(topologyId, correlationItem);
                            aggregationMap.put(logicalNodeIdentifier, logicalNode);
                        } else {
                            // add new physical node to existing logical node
                            YangInstanceIdentifier logicalIdentifier = topoStoreNode.getLogicalIdentifier();
                            newNode.setLogicalIdentifier(logicalIdentifier);
                            aggregationMap.get(logicalIdentifier).addPhysicalNode(newNode);
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete node from Aggregation map
     * @param identifiers Yang Instance Identifier
     * @param topologyId Topology Identification
     */
    public void processRemovedChanges(ArrayList<YangInstanceIdentifier> identifiers, final String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, PhysicalNode> physicalNodes = ts.getPhysicalNodes();
                for (YangInstanceIdentifier identifier : identifiers) {
                    PhysicalNode physicalNode = physicalNodes.remove(identifier);
                    // if identifier exists in topology store
                    if (null != physicalNode) {
                        YangInstanceIdentifier logicalIdentifier = physicalNode.getLogicalIdentifier();
                        // if physical node is part of some logical node
                        removePhysicalNodeFromLogicalNode(physicalNode, logicalIdentifier);
                    }
                }
            }
        }
    }

    private void removePhysicalNodeFromLogicalNode(PhysicalNode physicalNode,
            YangInstanceIdentifier logicalIdentifier) {
        if (null != logicalIdentifier) {
            LogicalNode logicalNode = this.aggregationMap.get(logicalIdentifier);
            ArrayList<PhysicalNode> aggregatedNodes = logicalNode.getPhysicalNodes();
            // if logical node consists only of 2 physical nodes
            if (2 == aggregatedNodes.size()) {
                aggregatedNodes.remove(physicalNode);
                PhysicalNode restNode = aggregatedNodes.iterator().next();
                restNode.setLogicalIdentifier(null);
                aggregationMap.remove(logicalIdentifier);
            } else {
                aggregatedNodes.remove(physicalNode);
            }
        }
    }

    /**
     * @param updatedEntries
     * @param topologyId
     * @param topologyStores 
     */
    public void processUpdatedChanges_old(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries,
            String topologyId) {
        if (updatedEntries != null) {
            for (TopologyStore ts : topologyStores) {
                if(ts.getId().equals(topologyId)) {
                    for (Entry<YangInstanceIdentifier, PhysicalNode> updatedEntry : updatedEntries.entrySet()) {
                        PhysicalNode oldPhysicalNode = ts.getPhysicalNodes().get(updatedEntry.getKey());
                        if (oldPhysicalNode == null) {
                            throw new IllegalStateException("Updated physical node not found in Topology Store");
                        }
                        if (oldPhysicalNode.getLeafNode().equals(updatedEntry.getValue().getLeafNode())) {
                            oldPhysicalNode.setNode(updatedEntry.getValue().getNode());
                        } else {
                            removePhysicalNodeFromLogicalNode(oldPhysicalNode,
                                    oldPhysicalNode.getLogicalIdentifier());
                        }
                    }
                } else {
                    // option 1
                    //createLogicalNodeFromPhysicalNodes(updatedEntries, topologyId, ts);
                }
            }
        }
    }

    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries,
            String topologyId) {
        for (Entry<YangInstanceIdentifier, PhysicalNode> updatedEntry : updatedEntries.entrySet()) {
            for (TopologyStore ts : topologyStores) {
                if (ts.getId().equals(topologyId)) {
                    PhysicalNode oldPhysicalNode = ts.getPhysicalNodes().get(updatedEntry.getKey());
                    if (oldPhysicalNode == null) {
                        throw new IllegalStateException("Updated physical node not found in Topology Store");
                    }
                    if(oldPhysicalNode.getLeafNode().equals(updatedEntry.getValue().getLeafNode())) {
                        oldPhysicalNode.setNode(updatedEntry.getValue().getNode());
                    } else {
                        removePhysicalNodeFromLogicalNode(oldPhysicalNode, oldPhysicalNode.getLogicalIdentifier());
                    }
                }
            }
            createAggregatedNodes(updatedEntry.getValue(), topologyId);
        }
    }
}
