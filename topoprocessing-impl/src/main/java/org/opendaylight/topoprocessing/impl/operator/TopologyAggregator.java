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
 * Class handling aggregation correlation
 * @author matus.marko
 * @author martin.uhlir
 */
public class TopologyAggregator implements TopologyOperator {

    private Map<YangInstanceIdentifier,LogicalNode> aggregationMap = new AggregationMap();
    private IdentifierGenerator idGenerator;
    private CorrelationItemEnum correlationItem;
    private List<TopologyStore> topologyStores;

    /**
     * Constructor
     * @param correlationItem
     * @param topologyStores
     * @param idGenerator 
     */
    public TopologyAggregator(CorrelationItemEnum correlationItem, List<TopologyStore> topologyStores,
                              IdentifierGenerator idGenerator) {
        this.correlationItem = correlationItem;
        this.topologyStores = topologyStores;
        this.idGenerator = idGenerator;
    }

    @Override
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

    private void createAggregatedNodes(PhysicalNode newNode,String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (!ts.getId().equals(topologyId)) {
                for (Entry<YangInstanceIdentifier, PhysicalNode> topoStoreEntry : ts.getPhysicalNodes().entrySet()) {
                    PhysicalNode topoStoreNode = topoStoreEntry.getValue();
                    if (topoStoreNode.getLeafNode().equals(newNode.getLeafNode())) {
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
                            aggregationMap.put(logicalNodeIdentifier, logicalNode);
                        } else {
                            // add new physical node into existing logical node
                            YangInstanceIdentifier logicalIdentifier = topoStoreNode.getLogicalIdentifier();
                            newNode.setLogicalIdentifier(logicalIdentifier);
                            LogicalNode logicalNode = aggregationMap.get(logicalIdentifier);
                            logicalNode.addPhysicalNode(newNode);
                            aggregationMap.put(logicalIdentifier, logicalNode);
                            return;
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
    @Override
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
            aggregatedNodes.remove(physicalNode);
            // if logical node consists only of 1 physical node
            if (1 == aggregatedNodes.size()) {
                PhysicalNode restNode = aggregatedNodes.iterator().next();
                restNode.setLogicalIdentifier(null);
                aggregationMap.remove(logicalIdentifier);
            } else {
                aggregationMap.put(logicalIdentifier, logicalNode);
            }
        }
    }

    @Override
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
                        createAggregatedNodes(updatedEntry.getValue(), topologyId);
                    }
                }
            }
        }
    }
}
