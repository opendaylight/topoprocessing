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

import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author martin.uhlir
 *
 */
public class UnificationAggregator implements TopologyOperator {

    private AggregationMap unificationMap = new AggregationMap();
    private IdentifierGenerator idGenerator;
    private CorrelationItemEnum correlationItem;
    private List<TopologyStore> topologyStores;


    public UnificationAggregator(CorrelationItemEnum correlationItem, List<TopologyStore> topologyStores,
            IdentifierGenerator idGenerator) {
        this.correlationItem = correlationItem;
        this.topologyStores = topologyStores;
        this.idGenerator = idGenerator;
    }

    @Override
    public AggregationMap processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries,
            String topologyId) {
        for (Entry<YangInstanceIdentifier, PhysicalNode> createdEntry : createdEntries.entrySet()) {
            for (TopologyStore ts : topologyStores) {
                if (ts.getId().equals(topologyId)) {
                    ts.getPhysicalNodes().put(createdEntry.getKey(), createdEntry.getValue());
                    // create logical node with one physical node
                    createAggregatedNodes(createdEntry.getValue(), topologyId);
                }
            }
            createAggregatedNodes(createdEntry.getValue(), topologyId);
        }
        return unificationMap;
    }

    /**
     * Creates either:
     *  1) aggregated (logical) node from physical nodes in TopologyStore and the new physical node or
     *  2) one aggregated (logical) node from from one physical node
     * @param newNode - new physical node
     * @param topologyId - id of topology to which this node belongs to
     */
    private void createAggregatedNodes(PhysicalNode newNode, String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (!ts.getId().equals(topologyId)) {
                for (Entry<YangInstanceIdentifier, PhysicalNode> topoStoreEntry : ts.getPhysicalNodes().entrySet()) {
                    PhysicalNode topoStoreNode = topoStoreEntry.getValue();
                    if (topoStoreNode.getLeafNode().getValue().equals(newNode.getLeafNode().getValue())) {
                        // no previous aggregation on this node
                        if (topoStoreNode.getLogicalIdentifier() == null) {
                            YangInstanceIdentifier logicalNodeIdentifier =
                                    idGenerator.getNextIdentifier(topologyId, correlationItem);
                            topoStoreNode.setLogicalIdentifier(logicalNodeIdentifier);
                            newNode.setLogicalIdentifier(logicalNodeIdentifier);
                            // create new logical node
                            List<PhysicalNode> nodesToAggregate = new ArrayList<>();
                            nodesToAggregate.add(newNode);
                            nodesToAggregate.add(topoStoreNode);
                            LogicalNode logicalNode = new LogicalNode(nodesToAggregate);
                            unificationMap.put(logicalNodeIdentifier, logicalNode);
                        } else {
                            // add new physical node into existing logical node
                            YangInstanceIdentifier logicalIdentifier = topoStoreNode.getLogicalIdentifier();
                            newNode.setLogicalIdentifier(logicalIdentifier);
                            LogicalNode logicalNode = unificationMap.get(logicalIdentifier);
                            logicalNode.addPhysicalNode(newNode);
                            unificationMap.put(logicalIdentifier, logicalNode);
                            return;
                        }
                    }
                }
            } else {
                // create Logical Node with one Physical Node
                List<PhysicalNode> nodesToAggregate = new ArrayList<>();
                nodesToAggregate.add(newNode);
                LogicalNode logicalNode = new LogicalNode(nodesToAggregate);
                YangInstanceIdentifier logicalNodeIdentifier =
                        idGenerator.getNextIdentifier(topologyId, correlationItem);
                unificationMap.put(logicalNodeIdentifier, logicalNode);
            }
        }
    }


    @Override
    public AggregationMap processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
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
        return unificationMap;
    }

    private void removePhysicalNodeFromLogicalNode(PhysicalNode physicalNode,
            YangInstanceIdentifier logicalIdentifier) {
        if (null != logicalIdentifier) {
            LogicalNode logicalNode = unificationMap.get(logicalIdentifier);
            List<PhysicalNode> aggregatedNodes = logicalNode.getPhysicalNodes();
            // remove physical node from corresponding logical node
            aggregatedNodes.remove(physicalNode);
            // if the last physical node was removed from the logical node
            if (aggregatedNodes.size() == 0) {
                unificationMap.remove(logicalIdentifier);
            } else {
                // replace the old logical node with new one
                unificationMap.put(logicalIdentifier, logicalNode);
            }
        }
    }

    @Override
    public AggregationMap processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries,
            String topologyId) {
        for (Entry<YangInstanceIdentifier, PhysicalNode> updatedEntry : updatedEntries.entrySet()) {
            for (TopologyStore ts : topologyStores) {
                if (ts.getId().equals(topologyId)) {
                    PhysicalNode oldPhysicalNode = ts.getPhysicalNodes().get(updatedEntry.getKey());
                    if (oldPhysicalNode == null) {
                        throw new IllegalStateException("Updated physical node not found in Topology Store");
                    }
                    YangInstanceIdentifier logicalIdentifier = oldPhysicalNode.getLogicalIdentifier();
                    if(oldPhysicalNode.getLeafNode().equals(updatedEntry.getValue().getLeafNode())) {
                        // replace old value of the node with the new value
                        oldPhysicalNode.setNode(updatedEntry.getValue().getNode());
                        if (logicalIdentifier != null) {
                            unificationMap.markUpdated(logicalIdentifier, unificationMap.get(logicalIdentifier));
                        }
                    } else {
                        removePhysicalNodeFromLogicalNode(oldPhysicalNode, logicalIdentifier);
                        createAggregatedNodes(updatedEntry.getValue(), topologyId);
                    }
                }
            }
        }
        return unificationMap;
    }
}
